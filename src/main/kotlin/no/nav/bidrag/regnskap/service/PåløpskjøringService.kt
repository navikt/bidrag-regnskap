package no.nav.bidrag.regnskap.service

import com.google.common.collect.Lists
import io.micrometer.core.instrument.LongTaskTimer
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import no.nav.bidrag.domene.enums.regnskap.Årsakskode
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.fil.overføring.FiloverføringTilElinKlient
import no.nav.bidrag.regnskap.fil.påløp.PåløpsfilGenerator
import no.nav.bidrag.regnskap.persistence.bucket.GcpFilBucket
import no.nav.bidrag.regnskap.persistence.entity.Driftsavvik
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import no.nav.bidrag.transport.regnskap.vedlikeholdsmodus.Vedlikeholdsmodus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

private val LOGGER = LoggerFactory.getLogger(PåløpskjøringService::class.java)

private const val PARTISJONSSTØRRELSE = 1000

@Service
class PåløpskjøringService(
    private val oppdragsperiodeRepo: OppdragsperiodeRepository,
    private val persistenceService: PersistenceService,
    private val manglendeKonteringerService: ManglendeKonteringerService,
    private val gcpFilBucket: GcpFilBucket,
    private val filoverføringTilElinKlient: FiloverføringTilElinKlient,
    private val skattConsumer: SkattConsumer,
    private val meterRegistry: MeterRegistry,
    @Autowired(required = false) private val lyttere: List<PåløpskjøringLytter> = emptyList(),
) {

    @Transactional
    fun hentPåløp() = persistenceService.hentIkkeKjørtePåløp().minByOrNull { it.forPeriode }

    fun startPåløpskjøring(påløp: Påløp, schedulertKjøring: Boolean, genererFil: Boolean) {
        if (påløp.startetTidspunkt != null) {
            LOGGER.warn("Påløpskjøring har satt startet tidspunkt! Dette kan være grunnet allerede kjørende påløp. Starter derfor ikke nytt påløp.")
            return
        }

        medLyttere { it.påløpStartet(påløp, schedulertKjøring, genererFil) }
        try {
            validerDriftsavvik(påløp, schedulertKjøring)
            val longTaskTimer = LongTaskTimer.builder("palop-kjoretid").register(meterRegistry).start()
            persistenceService.registrerPåløpStartet(påløp.påløpId, LocalDateTime.now())

            if (genererFil) {
                endreElinVedlikeholdsmodus(Årsakskode.PAALOEP_GENERERES, "Påløp for ${påløp.forPeriode} genereres hos NAV.")
            }

            val utsatteEllerFeiledeOppdragsperioder = opprettKonteringerForAlleOppdragsperioderSomIkkeHarOpprettetAlleKonteringer(påløp)

            if (genererFil) {
                genererPåløpsfil(påløp)
            }

            settOverføringstidspunktPåKonteringer(påløp)
            opprettKonteringerForAlleUtsatteEllerFeiledeOppdragsperioder(utsatteEllerFeiledeOppdragsperioder, påløp)
            avsluttDriftsavvik(påløp)
            fullførPåløp(påløp)

            if (genererFil) {
                endreElinVedlikeholdsmodus(Årsakskode.PAALOEP_LEVERT, "Påløp for ${påløp.forPeriode} er ferdig generert fra NAV.")
            }

            medLyttere { it.påløpFullført(påløp) }
            Metrics.timer("palop-kjoretid-ferdig").record<Long> { longTaskTimer.stop() }
        } catch (e: Error) {
            medLyttere { it.påløpFeilet(påløp, e.toString()) }
            throw e
        } catch (e: RuntimeException) {
            medLyttere { it.påløpFeilet(påløp, e.toString()) }
            throw e
        }
    }

    fun validerDriftsavvik(påløp: Påløp, schedulertKjøring: Boolean) {
        val driftsavvikListe = persistenceService.hentAlleAktiveDriftsavvik()
        if (driftsavvikListe.any { it.påløpId != påløp.påløpId }) {
            LOGGER.error("Det finnes aktive driftsavvik som ikke er knyttet til påløpet! Kan derfor ikke starte påløpskjøring!")
            throw IllegalStateException("Det finnes aktive driftsavvik som ikke er knyttet til påløpet! Kan derfor ikke starte påløpskjøring!")
        }
        if (driftsavvikListe.isEmpty()) {
            persistenceService.lagreDriftsavvik(opprettDriftsavvik(påløp, schedulertKjøring))
        }
    }

    private fun opprettDriftsavvik(påløp: Påløp, schedulertKjøring: Boolean): Driftsavvik {
        return Driftsavvik(
            påløpId = påløp.påløpId,
            tidspunktFra = LocalDateTime.now(),
            opprettetAv = if (schedulertKjøring) "Automatisk påløpskjøringer" else "Manuel påløpskjøring (REST)",
            årsak = "Påløpskjøring",
        )
    }

    private fun endreElinVedlikeholdsmodus(årsakskode: Årsakskode, kommentar: String) {
        skattConsumer.oppdaterVedlikeholdsmodus(Vedlikeholdsmodus(true, årsakskode, kommentar))
    }

    fun opprettKonteringerForAlleOppdragsperioderSomIkkeHarOpprettetAlleKonteringer(påløp: Påløp): List<Int> {
        val oppdragsperioder = ArrayList(oppdragsperiodeRepo.hentAlleOppdragsperioderSomIkkeHarOpprettetAlleKonteringer())
        var antallBehandlet = 0
        val utsatteEllerFeiledeOppdragsperioder = mutableListOf<Int>()

        medLyttere { it.rapporterOppdragsperioderBehandlet(påløp, antallBehandlet, oppdragsperioder.size) }

        Lists.partition(oppdragsperioder, PARTISJONSSTØRRELSE).parallelStream().forEach { oppdragsperiodeIds ->
            utsatteEllerFeiledeOppdragsperioder.addAll(
                manglendeKonteringerService.opprettKonteringerForOppdragsperiode(
                    påløp,
                    oppdragsperiodeIds,
                ),
            )
            antallBehandlet += oppdragsperiodeIds.size
            medLyttere { it.rapporterOppdragsperioderBehandlet(påløp, antallBehandlet, oppdragsperioder.size) }
            LOGGER.info(
                "Opprettet konteringer for $antallBehandlet av ${oppdragsperioder.size} oppdragsperioder i påløpskjøring for periode: ${påløp.forPeriode}",
            )
        }

        medLyttere { it.oppdragsperioderBehandletFerdig(påløp, oppdragsperioder.size) }

        return utsatteEllerFeiledeOppdragsperioder
    }

    fun opprettKonteringerForAlleUtsatteEllerFeiledeOppdragsperioder(utsatteEllerFeiledeOppdragsperioder: List<Int>, påløp: Påløp) {
        medLyttere { it.rapporterAntallUtsatteEllerFeiledeKonteringer(påløp, utsatteEllerFeiledeOppdragsperioder.size) }

        manglendeKonteringerService.opprettKonteringerForUtsatteOgFeiledeOppdragsperiode(
            påløp,
            utsatteEllerFeiledeOppdragsperioder,
        )

        medLyttere { it.rapporterAntallUtsatteEllerFeiledeKonteringerFerdig(påløp) }
    }

    fun genererPåløpsfil(påløp: Påløp) {
        LOGGER.info("Starter generering av påløpsfil...")
        medLyttere { it.generererFil(påløp) }
        skrivPåløpsfilOgLastOppPåFilsluse(påløp)
        LOGGER.info("Påløpsfil er ferdig skrevet for periode ${påløp.forPeriode} og lastet opp til filsluse.")
    }

    private fun settOverføringstidspunktPåKonteringer(påløp: Påløp) {
        val timestamp = LocalDateTime.now()
        var antallBehandlet = 0

        LOGGER.debug("Starter å sette overføringstidspunkt konteringer.")
        val konteringer = ArrayList(persistenceService.hentAlleIkkeOverførteKonteringer())
        Lists.partition(konteringer, PARTISJONSSTØRRELSE).forEach { konteringerPartition ->
            konteringerPartition.forEach {
                it.overføringstidspunkt = timestamp
                it.behandlingsstatusOkTidspunkt = timestamp
            }
            persistenceService.lagreKonteringer(konteringerPartition)
            antallBehandlet += konteringerPartition.size

            medLyttere { it.rapporterKonteringerFullført(påløp, antallBehandlet, konteringer.size) }
        }

        medLyttere { it.konteringerFullførtFerdig(påløp, konteringer.size) }
        LOGGER.debug("Fullført setting av overføringstidspunkt for konteringer.")
    }

    private fun skrivPåløpsfilOgLastOppPåFilsluse(påløp: Påløp) {
        val påløpsfilGenerator = PåløpsfilGenerator(gcpFilBucket, filoverføringTilElinKlient, persistenceService)
        påløpsfilGenerator.skrivPåløpsfilOgLastOppPåFilsluse(påløp, lyttere)
    }

    private fun medLyttere(lytterConsumer: Consumer<PåløpskjøringLytter>) = lyttere.forEach(lytterConsumer)

    fun fullførPåløp(påløp: Påløp) {
        persistenceService.lagrePåløp(påløp.copy(fullførtTidspunkt = LocalDateTime.now()))
    }

    fun avsluttDriftsavvik(påløp: Påløp) {
        val driftsavvik = persistenceService.hentDriftsavvikForPåløp(påløp.påløpId) ?: error("Fant ikke driftsavvik på ID: ${påløp.påløpId}")
        persistenceService.lagreDriftsavvik(driftsavvik.copy(tidspunktTil = LocalDateTime.now()))
    }

    fun startManuellOverføringPåløp(dato: LocalDate) {
        val nowFormattert = dato.format(DateTimeFormatter.ofPattern("yyMMdd")).toString()
        val påløpsMappe = "påløp/"
        val påløpsfilnavn = "paaloop_D$nowFormattert.xml"

        filoverføringTilElinKlient.lastOppFilTilFilsluse(påløpsMappe, påløpsfilnavn)
    }
}

interface PåløpskjøringLytter {
    fun påløpStartet(påløp: Påløp, schedulertKjøring: Boolean, genererFil: Boolean)
    fun rapporterOppdragsperioderBehandlet(påløp: Påløp, antallBehandlet: Int, antallOppdragsperioder: Int)

    fun rapporterAntallUtsatteEllerFeiledeKonteringer(påløp: Påløp, antallOppdragsperioder: Int)

    fun rapporterAntallUtsatteEllerFeiledeKonteringerFerdig(påløp: Påløp)

    fun oppdragsperioderBehandletFerdig(påløp: Påløp, antallOppdragsperioder: Int)

    fun generererFil(påløp: Påløp)

    fun rapportertKonteringerSkrevetTilFil(påløp: Påløp, antallSkrevetTilFil: Int, antallKonteringerTotalt: Int)

    fun konteringerSkrevetTilFilFerdig(påløp: Påløp, antallKonteringerTotalt: Int)

    fun påløpFullført(påløp: Påløp)

    fun påløpFeilet(påløp: Påløp, feilmelding: String)

    fun rapporterKonteringerFullført(påløp: Påløp, antallFullført: Int, totaltAntall: Int)

    fun konteringerFullførtFerdig(påløp: Påløp, totaltAntall: Int)

    fun lastOppFilTilGcpBucket(påløp: Påløp, melding: String)

    fun lastOppFilTilFilsluse(påløp: Påløp, melding: String)
}
