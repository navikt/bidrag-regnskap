package no.nav.bidrag.regnskap.slack

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.bidrag.regnskap.service.PåløpskjøringLytter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.SocketTimeoutException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Optional

private val LOGGER = KotlinLogging.logger {}

@Service
class SlackPåløpVarsler(
    private val slackService: SlackService,
    @Value("\${NAIS_CLIENT_ID}") private val clientId: String,
) : PåløpskjøringLytter {

    val antallKlumper = 20
    var pågåendePåløp: PågåendePåløp? = null
    override fun påløpStartet(påløp: Påløp, schedulertKjøring: Boolean, genererFil: Boolean) {
        pågåendePåløp?.melding?.svarITråd("Nytt påløp startet...")

        val melding = slackService.sendMelding(
            ":open_file_folder: Påløpskjøring er startet for ${påløp.forPeriode}!\nSkedulert: $schedulertKjøring\nGenerer fil: $genererFil\nMiljø: $clientId",
        )
        pågåendePåløp = PågåendePåløp(påløp = påløp, schedulertKjøring = schedulertKjøring, genererFil = genererFil, melding = melding)
    }

    override fun rapporterOppdragsperioderBehandlet(påløp: Påløp, antallBehandlet: Int, antallOppdragsperioder: Int) {
        try {
            val varsel = pågåendePåløp(påløp)
            if (varsel != null) {
                if (!varsel.skalOppdatereKonteringerMelding()) {
                    return
                }
                varsel.registrerObservasjon(antallBehandlet)
                val melding = "Opprettet konteringer for $antallBehandlet av $antallOppdragsperioder oppdragsperioder\n${
                    fremdriftsindikator(
                        antallBehandlet,
                        antallOppdragsperioder,
                    )
                }\nTid pr periode: ${varsel.millisekunderPrPeriode().map { it.toString() }.orElse("?")} ms\nSist oppdatert: ${LocalDateTime.now()}"
                if (varsel.konteringerMelding == null) {
                    varsel.konteringerMelding =
                        pågåendePåløp?.melding?.svarITråd(melding)
                } else {
                    varsel.konteringerMelding?.oppdaterMelding(melding)
                }
            }
        } catch (e: SocketTimeoutException) {
            LOGGER.error { "Oppdatering av slackmelding feilet grunnet: ${e.message}" }
        }
    }

    override fun rapporterAntallUtsatteEllerFeiledeKonteringer(påløp: Påløp, antallOppdragsperioder: Int) {
        pågåendePåløp?.melding?.svarITråd("Oppretter konteringer for $antallOppdragsperioder oppdragsperioder med utsatte eller feilede konteringer.")
    }

    override fun rapporterAntallUtsatteEllerFeiledeKonteringerFerdig(påløp: Påløp) {
        pågåendePåløp(påløp)?.påløpsfilMelding
            ?.oppdaterMelding("Ferdig med å opprette konteringer for oppdragsperioder med utsatte eller feilede konteringer!")
    }

    override fun oppdragsperioderBehandletFerdig(påløp: Påløp, antallOppdragsperioder: Int) {
        pågåendePåløp(påløp)?.konteringerMelding?.oppdaterMelding(
            "Opprettet konteringer for $antallOppdragsperioder oppdragsperioder. Fullført tidspunkt: ${LocalDateTime.now()}",
        )
    }

    override fun generererFil(påløp: Påløp) {
        pågåendePåløp?.melding?.svarITråd("Genererer fil...")
    }

    override fun rapportertKonteringerSkrevetTilFil(påløp: Påløp, antallSkrevetTilFil: Int, antallKonteringerTotalt: Int) {
        val varsel = pågåendePåløp(påløp)

        if (varsel != null) {
            varsel.registrerObservasjon(antallSkrevetTilFil)
            val melding = "Skrevet $antallSkrevetTilFil av $antallKonteringerTotalt konteringer til fil" +
                "\n${fremdriftsindikator(antallSkrevetTilFil, antallKonteringerTotalt)}" +
                "\nTid pr kontering: ${varsel.millisekunderPrPeriode().map { it.toString() }.orElse("?")} ms"
            if (varsel.påløpsfilMelding == null) {
                varsel.påløpsfilMelding =
                    pågåendePåløp?.melding?.svarITråd(melding)
            } else {
                varsel.påløpsfilMelding?.oppdaterMelding(melding)
            }
        }
    }

    override fun konteringerSkrevetTilFilFerdig(påløp: Påløp, antallKonteringerTotalt: Int) {
        pågåendePåløp(
            påløp,
        )?.påløpsfilMelding?.oppdaterMelding(
            "Påløpet har skrevet ferdig fil med $antallKonteringerTotalt konteringer! Fullført tidspunkt: ${LocalDateTime.now()}",
        )
    }

    override fun rapporterKonteringerFullført(påløp: Påløp, antallFullført: Int, totaltAntall: Int) {
        val varsel = pågåendePåløp(påløp)

        if (varsel != null) {
            if (!varsel.skalOppdatereKonteringerMelding()) {
                return
            }
            varsel.registrerObservasjon(antallFullført)
            val melding = "Påløpet har fullført $antallFullført av $totaltAntall konteringer og satt overføringstidspunkt." +
                "\n${fremdriftsindikator(antallFullført, totaltAntall)}" +
                "\nTid pr kontering: ${varsel.millisekunderPrPeriode().map { it.toString() }.orElse("?")} ms"
            if (varsel.konteringerFullførtMelding == null) {
                varsel.konteringerFullførtMelding =
                    pågåendePåløp?.melding?.svarITråd(melding)
            } else {
                varsel.konteringerFullførtMelding?.oppdaterMelding(melding)
            }
        }
    }

    override fun konteringerFullførtFerdig(påløp: Påløp, totaltAntall: Int) {
        pågåendePåløp(
            påløp,
        )?.konteringerFullførtMelding?.oppdaterMelding(
            "Påløpet har satt overføringstidspunkt for $totaltAntall konteringer. Fullført tidspunkt: ${LocalDateTime.now()}",
        )
    }

    override fun påløpFullført(påløp: Påløp) {
        val varsel = pågåendePåløp(påløp)

        varsel?.melding?.svarITråd("Påløp er fullført!")
        varsel?.melding?.oppdaterMelding(
            ":file_folder: Påløpskjøring er fullført for ${påløp.forPeriode}!\nSkedulert: ${varsel.schedulertKjøring}\nGenerer fil: ${varsel.genererFil}\nMiljø: $clientId",
        )
    }

    override fun påløpFeilet(påløp: Påløp, feilmelding: String) {
        pågåendePåløp?.melding?.svarITråd("Påløp feilet: $feilmelding")
    }

    override fun lastOppFilTilFilsluse(påløp: Påløp, melding: String) {
        val varsel = pågåendePåløp(påløp)
        if (varsel != null) {
            if (varsel.lastOppFilTilGcpMelding == null) {
                varsel.lastOppFilTilGcpMelding =
                    pågåendePåløp?.melding?.svarITråd(melding)
            } else {
                varsel.lastOppFilTilGcpMelding?.oppdaterMelding(melding)
            }
        }
    }

    override fun lastOppFilTilGcpBucket(påløp: Påløp, melding: String) {
        val varsel = pågåendePåløp(påløp)
        if (varsel != null) {
            if (varsel.lastOppFilTilFilsluseMelding == null) {
                varsel.lastOppFilTilFilsluseMelding =
                    pågåendePåløp?.melding?.svarITråd(melding)
            } else {
                varsel.lastOppFilTilFilsluseMelding?.oppdaterMelding(melding)
            }
        }
    }

    private fun pågåendePåløp(påløp: Påløp) = if (påløp.equals(pågåendePåløp?.påløp)) pågåendePåløp else null

    private fun fremdriftsindikator(antall: Int, totalt: Int): String {
        val fyllteKlumper = if (totalt > 0) (antall * antallKlumper) / totalt else antallKlumper
        val prosent = if (totalt > 0) (100 * antall) / totalt else 100

        return "`[${"█".repeat(fyllteKlumper)}${" ".repeat(antallKlumper - fyllteKlumper)}]` $prosent%"
    }

    class PågåendePåløp(
        val påløp: Påløp,
        val schedulertKjøring: Boolean,
        val genererFil: Boolean,
        val melding: SlackService.SlackMelding,
    ) {
        val oppdateringInterval = Duration.ofSeconds(30)
        var konteringerMelding: SlackService.SlackMelding? = null
        var påløpsfilMelding: SlackService.SlackMelding? = null
        var konteringerFullførtMelding: SlackService.SlackMelding? = null
        var lastOppFilTilGcpMelding: SlackService.SlackMelding? = null
        var lastOppFilTilFilsluseMelding: SlackService.SlackMelding? = null
        var nesteOppdateringKonteringerMelding: Instant? = Instant.now()
        var nestSisteObservasjon: PåløpObservasjon = PåløpObservasjon(antallBehandlet = 0)
        var sisteObservasjon: PåløpObservasjon = PåløpObservasjon(antallBehandlet = 0)

        fun skalOppdatereKonteringerMelding(): Boolean {
            if (konteringerMelding == null ||
                !Instant.now().isBefore(nesteOppdateringKonteringerMelding)
            ) {
                nesteOppdateringKonteringerMelding = Instant.now().plus(oppdateringInterval)
                return true
            }
            return false
        }

        fun registrerObservasjon(antallBehandlet: Int, tidspunkt: Instant = Instant.now()) {
            if (antallBehandlet > sisteObservasjon.antallBehandlet) {
                nestSisteObservasjon = sisteObservasjon
                sisteObservasjon = PåløpObservasjon(tidspunkt, antallBehandlet)
            }
        }

        fun millisekunderPrPeriode(): Optional<Int> {
            val behandletDelta = sisteObservasjon.antallBehandlet - nestSisteObservasjon.antallBehandlet
            if (behandletDelta <= 0) {
                return Optional.empty()
            }
            return Optional.of((ChronoUnit.MILLIS.between(nestSisteObservasjon.tidspunkt, sisteObservasjon.tidspunkt) / behandletDelta).toInt())
        }
    }

    class PåløpObservasjon(
        val tidspunkt: Instant = Instant.now(),
        val antallBehandlet: Int,
    )
}
