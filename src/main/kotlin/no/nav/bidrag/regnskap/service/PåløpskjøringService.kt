package no.nav.bidrag.regnskap.service

import com.google.common.collect.Lists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.dto.enumer.Årsakskode
import no.nav.bidrag.regnskap.dto.påløp.Vedlikeholdsmodus
import no.nav.bidrag.regnskap.fil.påløp.PåløpsfilGenerator
import no.nav.bidrag.regnskap.persistence.entity.Driftsavvik
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.OverføringKontering
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.Error
import java.lang.RuntimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.ArrayList

private val LOGGER = LoggerFactory.getLogger(PåløpskjøringService::class.java)

private const val partisjonStørrelse = 1000

@Service
class PåløpskjøringService(
    private val oppdragsperiodeRepo: OppdragsperiodeRepository,
    private val persistenceService: PersistenceService,
    private val manglendeKonteringerService: ManglendeKonteringerService,
    private val påløpsfilGenerator: PåløpsfilGenerator,
    private val skattConsumer: SkattConsumer,
    @Autowired(required = false) private val lyttere: List<PåløpskjøringLytter> = emptyList()
) {

    private lateinit var påløpskjøringJob: Job

    @Transactional
    fun hentPåløp() = persistenceService.hentIkkeKjørtePåløp().minByOrNull { it.forPeriode }

    fun startPåløpskjøring(påløp: Påløp, schedulertKjøring: Boolean, genererFil: Boolean) {
        if (påløp.startetTidspunkt != null) {
            return
        }
        lyttere.forEach { it.påløpStartet(påløp, schedulertKjøring, genererFil) }
        try {
            validerDriftsavvik(påløp, schedulertKjøring)
            persistenceService.registrerPåløpStartet(påløp.påløpId, LocalDateTime.now())

            if (genererFil) {
                endreElinVedlikeholdsmodus(
                    Årsakskode.PAALOEP_GENERERES,
                    "Påløp for ${påløp.forPeriode} genereres hos NAV."
                )
            }
            opprettKonteringerForAlleOppdragsperioderSomIkkeHarOpprettetAlleKonteringer(påløp)
            genererPåløpsfil(påløp, genererFil)
            fullførPåløp(påløp)
            if (genererFil) {
                endreElinVedlikeholdsmodus(
                    Årsakskode.PAALOEP_LEVERT,
                    "Påløp for ${påløp.forPeriode} er ferdig generert fra NAV."
                )
            }
            avsluttDriftsavvik(påløp)
            lyttere.forEach { it.påløpFullført(påløp) }
        } catch (e: Error) {
            lyttere.forEach { it.påløpFeilet(påløp, e.toString()) }
            throw e
        } catch (e: RuntimeException) {
            lyttere.forEach { it.påløpFeilet(påløp, e.toString()) }
            throw e
        }
    }

    fun stoppPågåendePåløpskjøring() {
        if (this::påløpskjøringJob.isInitialized) {
            påløpskjøringJob.cancel()
        }
    }

    @Transactional
    fun validerDriftsavvik(påløp: Påløp, schedulertKjøring: Boolean) {
        val driftsavvikListe = persistenceService.hentAlleAktiveDriftsavvik()
        if (driftsavvikListe.any { it.påløpId != påløp.påløpId }) {
            LOGGER.error("Det finnes aktive driftsavvik som ikke er knyttet til påløpet! Kan derfor ikke starte påløpskjøring!")
            throw IllegalStateException("Det finnes aktive driftsavvik som ikke er knyttet til påløpet! Kan derfor ikke starte påløpskjøring!")
        }
        if (driftsavvikListe.isEmpty()) {
            persistenceService.lagreDriftsavvik(
                opprettDriftsavvik(
                    påløp,
                    schedulertKjøring
                )
            )
        }
    }

    private fun opprettDriftsavvik(påløp: Påløp, schedulertKjøring: Boolean): Driftsavvik {
        return Driftsavvik(
            påløpId = påløp.påløpId,
            tidspunktFra = LocalDateTime.now(),
            opprettetAv = if (schedulertKjøring) "Automatisk påløpskjøringer" else "Manuel påløpskjøring (REST)",
            årsak = "Påløpskjøring"
        )
    }

    private fun endreElinVedlikeholdsmodus(årsakskode: Årsakskode, kommentar: String) {
        skattConsumer.oppdaterVedlikeholdsmodus(Vedlikeholdsmodus(true, årsakskode, kommentar))
    }

    fun opprettKonteringerForAlleOppdragsperioderSomIkkeHarOpprettetAlleKonteringer(påløp: Påløp) {
        val påløpsPeriode = LocalDate.parse(påløp.forPeriode + "-01")
        val oppdragsperioder = ArrayList(oppdragsperiodeRepo.hentAlleOppdragsperioderSomIkkeHarOpprettetAlleKonteringer())
        var antallBehandlet = 0

        lyttere.forEach { it.rapporterOppdragsperioderBehandlet(påløp, antallBehandlet, oppdragsperioder.size) }

        Lists.partition(oppdragsperioder, partisjonStørrelse).forEach { oppdragsperiodeIds ->
            val startTime = System.currentTimeMillis()
            manglendeKonteringerService.opprettKonteringerForAlleOppdragsperiodePartisjon(påløpsPeriode, oppdragsperiodeIds)
            LOGGER.info("TIDSBRUK opprettKonteringerForAlleOppdragsperiodePartisjon_ekstern: {}ms, Ledig minne: {}", System.currentTimeMillis() - startTime, Runtime.getRuntime().freeMemory())

            antallBehandlet += oppdragsperiodeIds.size
            lyttere.forEach { it.rapporterOppdragsperioderBehandlet(påløp, antallBehandlet, oppdragsperioder.size) }
        }

        lyttere.forEach { it.oppdragsperioderBehandletFerdig(påløp, oppdragsperioder.size) }
    }

    @Transactional
    fun genererPåløpsfil(påløp: Påløp, genererFil: Boolean) {
        LOGGER.info("Starter generering av påløpsfil...")
        val konteringer = persistenceService.hentAlleIkkeOverførteKonteringer()
        if (genererFil) {
            runBlocking { skrivPåløpsfilOgLastOppPåFilsluse(konteringer, påløp) }
        }
        settKonteringTilOverførtOgOpprettOverføringKontering(konteringer, påløp)
        LOGGER.info("Påløpsfil er ferdig skrevet med ${konteringer.size} konteringer og lastet opp til filsluse.")
    }

    private suspend fun skrivPåløpsfilOgLastOppPåFilsluse(konteringer: List<Kontering>, påløp: Påløp) = coroutineScope {
        påløpskjøringJob = launch {
            withContext(Dispatchers.IO) {
                påløpsfilGenerator.skrivPåløpsfilOgLastOppPåFilsluse(konteringer, påløp)
            }
        }
    }

    private fun settKonteringTilOverførtOgOpprettOverføringKontering(konteringer: List<Kontering>, påløp: Påløp) {
        val timestamp = LocalDateTime.now()
        konteringer.forEach {
            it.overføringstidspunkt = timestamp
            it.sendtIPåløpsperiode = påløp.forPeriode
            it.behandlingsstatusOkTidspunkt = timestamp
            persistenceService.lagreKontering(it)
            persistenceService.lagreOverføringKontering(
                OverføringKontering(kontering = it, tidspunkt = timestamp, kanal = "Påløpsfil")
            )
        }
    }

    @Transactional
    fun fullførPåløp(påløp: Påløp) {
        påløp.fullførtTidspunkt = LocalDateTime.now()
        persistenceService.lagrePåløp(påløp)
    }

    @Transactional
    fun avsluttDriftsavvik(påløp: Påløp) {
        val driftsavvik =
            persistenceService.hentDriftsavvikForPåløp(påløp.påløpId)
                ?: error("Fant ikke driftsavvik på ID: ${påløp.påløpId}")
        driftsavvik.tidspunktTil = LocalDateTime.now()
        persistenceService.lagreDriftsavvik(driftsavvik)
    }
}

interface PåløpskjøringLytter {
    fun påløpStartet(påløp: Påløp, schedulertKjøring: Boolean, genererFil: Boolean)
    fun rapporterOppdragsperioderBehandlet(påløp: Påløp, antallBehandlet: Int, antallOppdragsperioder: Int)

    fun oppdragsperioderBehandletFerdig(påløp: Påløp, antallOppdragsperioder: Int)

    fun generererFil(påløp: Påløp)

    fun påløpFullført(påløp: Påløp)

    fun påløpFeilet(påløp: Påløp, feilmelding: String)
}
