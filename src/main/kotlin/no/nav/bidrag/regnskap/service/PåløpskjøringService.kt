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
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.function.Consumer

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
        medLyttere { it.påløpStartet(påløp, schedulertKjøring, genererFil) }
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
            if (genererFil) {
                genererPåløpsfil(påløp)
            }
            settOverføringstidspunktPåKonteringer(påløp)
            fullførPåløp(påløp)
            if (genererFil) {
                endreElinVedlikeholdsmodus(
                    Årsakskode.PAALOEP_LEVERT,
                    "Påløp for ${påløp.forPeriode} er ferdig generert fra NAV."
                )
            }
            avsluttDriftsavvik(påløp)
            medLyttere { it.påløpFullført(påløp) }
        } catch (e: Error) {
            medLyttere { it.påløpFeilet(påløp, e.toString()) }
            throw e
        } catch (e: RuntimeException) {
            medLyttere { it.påløpFeilet(påløp, e.toString()) }
            throw e
        }
    }

    fun stoppPågåendePåløpskjøring() {
        if (this::påløpskjøringJob.isInitialized) {
            påløpskjøringJob.cancel()
        }
    }

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
        val oppdragsperioder =
            ArrayList(oppdragsperiodeRepo.hentAlleOppdragsperioderSomIkkeHarOpprettetAlleKonteringer())
        var antallBehandlet = 0

        medLyttere { it.rapporterOppdragsperioderBehandlet(påløp, antallBehandlet, oppdragsperioder.size) }

        Lists.partition(oppdragsperioder, partisjonStørrelse).forEach { oppdragsperiodeIds ->
            manglendeKonteringerService.opprettKonteringerForAlleOppdragsperiodePartisjon(
                påløpsPeriode,
                oppdragsperiodeIds
            )

            antallBehandlet += oppdragsperiodeIds.size
            medLyttere { it.rapporterOppdragsperioderBehandlet(påløp, antallBehandlet, oppdragsperioder.size) }
        }

        medLyttere { it.oppdragsperioderBehandletFerdig(påløp, oppdragsperioder.size) }
    }

    fun genererPåløpsfil(påløp: Påløp) {
        LOGGER.info("Starter generering av påløpsfil...")
        medLyttere { it.generererFil(påløp) }
        runBlocking { skrivPåløpsfilOgLastOppPåFilsluse(påløp) }
        LOGGER.info("Påløpsfil er ferdig skrevet for periode ${påløp.forPeriode} og lastet opp til filsluse.")
    }

    private fun settOverføringstidspunktPåKonteringer(påløp: Påløp) {
        val timestamp = LocalDateTime.now()
        var pageNumber = 0
        val pageSize = 10000
        var konteringerPage: Page<Kontering>

        LOGGER.info("Starter å sette overføringstidspunkt konteringer.")
        medLyttere { it.rapporterOverføringstidspunkt(påløp, "Starter oppdatering av overføringstidspunkt, behandlingsstatusOkTidspunkt og påløpsperiode på alle konteringer. Timestamp: $timestamp")}
//        do {
//            konteringerPage = persistenceService.hentAlleIkkeOverførteKonteringer(pageNumber, pageSize)
//            settKonteringTilOverførtOgOpprettOverføringKontering(konteringerPage, påløp, timestamp)
//            pageNumber++
//            medLyttere { it.rapporterKonteringerFullført(påløp, pageNumber, konteringerPage.totalPages, pageSize) }
//        } while (konteringerPage.hasNext())

        persistenceService.oppdaterAlleKonteringerMedOverføringstidspunktBehandlingsstatusOkTidspunktOgPåløpsperiode(timestamp, påløp.forPeriode)

        LOGGER.info("Fullført setting av overføringstidspunkt for konteringer.")
        medLyttere { it.rapporterOverføringstidspunkt(påløp, "Oppdatering av overføringstidspunkt, behandlingsstatusOkTidspunkt og påløpsperiode fullført på alle konteringer. Timestamp: ${LocalDateTime.now()}")}
    }

    private suspend fun skrivPåløpsfilOgLastOppPåFilsluse(påløp: Påløp) = coroutineScope {
        påløpskjøringJob = launch {
            withContext(Dispatchers.IO) {
                påløpsfilGenerator.skrivPåløpsfilOgLastOppPåFilsluse(påløp, lyttere)
            }
        }
    }

    private fun settKonteringTilOverførtOgOpprettOverføringKontering(
        konteringer: Page<Kontering>,
        påløp: Påløp,
        timestamp: LocalDateTime
    ) {
        konteringer.content.forEach {
            it.overføringstidspunkt = timestamp
            it.sendtIPåløpsperiode = påløp.forPeriode
            it.behandlingsstatusOkTidspunkt = timestamp
            persistenceService.lagreKontering(it)
        }
    }

    private inline fun medLyttere(lytterConsumer: Consumer<PåløpskjøringLytter>) = lyttere.forEach(lytterConsumer)

    fun fullførPåløp(påløp: Påløp) {
        påløp.fullførtTidspunkt = LocalDateTime.now()
        persistenceService.lagrePåløp(påløp)
    }

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

    fun rapportertKonteringerSkrevetTilFil(påløp: Påløp, antallSkrevetTilFil: Int, antallKonteringerTotalt: Int)

    fun påløpFullført(påløp: Påløp)

    fun påløpFeilet(påløp: Påløp, feilmelding: String)

    fun rapporterKonteringerFullført(påløp: Påløp, antallSiderFullført: Int, totaltAntallSider: Int, antallPerSide: Int)

    fun rapporterOverføringstidspunkt(påløp: Påløp, melding: String)
}
