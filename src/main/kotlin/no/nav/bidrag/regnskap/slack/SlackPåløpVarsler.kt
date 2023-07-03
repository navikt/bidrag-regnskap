package no.nav.bidrag.regnskap.slack

import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.bidrag.regnskap.service.PåløpskjøringLytter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional

@Service
class SlackPåløpVarsler(
    private val slackService: SlackService,
    @Value("\${NAIS_CLIENT_ID}") private val clientId: String
) : PåløpskjøringLytter {

    val antallKlumper = 20
    var pågåendePåløp: PågåendePåløp? = null
    override fun påløpStartet(påløp: Påløp, schedulertKjøring: Boolean, genererFil: Boolean) {
        pågåendePåløp?.melding?.svarITråd("Nytt påløp startet...")

        val melding = slackService.sendMelding(":open_file_folder: Påløpskjøring er startet for ${påløp.forPeriode}!\nSkedulert: $schedulertKjøring\nGenerer fil: $genererFil\nMiljø: $clientId")
        pågåendePåløp = PågåendePåløp(påløp = påløp, schedulertKjøring = schedulertKjøring, genererFil = genererFil, melding = melding)
    }

    override fun rapporterOppdragsperioderBehandlet(påløp: Påløp, antallBehandlet: Int, antallOppdragsperioder: Int) {
        val varsel = pågåendePåløp(påløp)
        if (varsel != null) {
            if (!varsel.skalOppdatereKonteringerMelding()) {
                return
            }
            varsel.registrerObservasjon(antallBehandlet)
            val melding = "Opprettet konteringer for $antallBehandlet av $antallOppdragsperioder oppdragsperioder\n${fremdriftsindikator(antallBehandlet, antallOppdragsperioder)}\nTid pr periode: ${varsel.millisekunderPrPeriode().map{it.toString()}.orElse("?")} ms"
            if (varsel.konteringerMelding == null) {
                varsel.konteringerMelding =
                    pågåendePåløp?.melding?.svarITråd(melding)
            } else {
                varsel.konteringerMelding?.oppdaterMelding(melding)
            }
        }
    }

    override fun oppdragsperioderBehandletFerdig(påløp: Påløp, antallOppdragsperioder: Int) {
        val varsel = pågåendePåløp(påløp)
        varsel?.konteringerMelding?.oppdaterMelding("Opprettet konteringer for $antallOppdragsperioder oppdragsperioder")
    }

    override fun generererFil(påløp: Påløp) {
        pågåendePåløp?.melding?.svarITråd("Genererer fil...")
    }

    override fun påløpFullført(påløp: Påløp) {
        val varsel = pågåendePåløp(påløp)

        varsel?.melding?.svarITråd("Påløp er fullført")
        varsel?.melding?.oppdaterMelding(":file_folder: Påløpskjøring er fullført for ${påløp.forPeriode}!\nSkedulert: ${varsel.schedulertKjøring}\nGenerer fil: ${varsel.genererFil}\nMiljø: $clientId")
    }

    override fun påløpFeilet(påløp: Påløp, feilmelding: String) {
        pågåendePåløp?.melding?.svarITråd("Påløp feilet: $feilmelding")
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
        val startTid: Instant = Instant.now(),
        val melding: SlackService.SlackMelding
    ) {
        val oppdateringInterval = Duration.ofSeconds(30)
        var konteringerMelding: SlackService.SlackMelding? = null
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
        val antallBehandlet: Int
    )
}
