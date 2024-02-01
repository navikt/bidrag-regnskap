package no.nav.bidrag.regnskap.hendelse.schedule.krav

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.regnskap.service.BehandlingsstatusService
import no.nav.bidrag.regnskap.slack.SlackService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

private val LOGGER = KotlinLogging.logger { }

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class SjekkAvBehandlingsstatusScheduler(
    private val behandlingsstatusService: BehandlingsstatusService,
    private val kravSchedulerUtils: KravSchedulerUtils,
    private val slackService: SlackService,
    private val meterRegistry: MeterRegistry,
    @Value("\${NAIS_CLIENT_ID}") private val clientId: String,
) {

    @Scheduled(cron = "\${scheduler.behandlingsstatus.cron}")
    @SchedulerLock(name = "skedulertSjekkAvBehandlingsstatus")
    @Transactional
    fun skedulertSjekkAvBehandlingsstatus() {
        LockAssert.assertLocked()
        LOGGER.info { "Starter schedulert sjekk av behandlingsstatus for allerede overførte konteringer." }
        if (kravSchedulerUtils.harAktivtDriftsavvik()) {
            LOGGER.warn { "Det finnes aktive driftsavvik. Starter derfor ikke sjekk av behandlingsstatus." }
            Gauge.builder("behandlingsstatus-feilet-for-antall") { -1 }.strongReference(true).register(meterRegistry)
            return
        } else if (kravSchedulerUtils.erVedlikeholdsmodusPåslått()) {
            LOGGER.warn { "Vedlikeholdsmodus er påslått! Starter derfor ikke sjekk av behandlingsstatus." }
            Gauge.builder("behandlingsstatus-feilet-for-antall") { -1 }.strongReference(true).register(meterRegistry)
            return
        }

        val konteringerSomIkkeHarFåttGodkjentBehandlingsstatus = behandlingsstatusService.hentKonteringerMedIkkeGodkjentBehandlingsstatus()

        if (konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.isEmpty()) {
            LOGGER.info { "Det finnes ingen konteringer som ikke har sjekket behandlingsstatus." }
            Gauge.builder("behandlingsstatus-feilet-for-antall") { 0 }.strongReference(true).register(meterRegistry)
            return
        }

        val feiledeOverføringer: HashMap<String, String> =
            behandlingsstatusService.hentBehandlingsstatusForIkkeGodkjenteKonteringer(konteringerSomIkkeHarFåttGodkjentBehandlingsstatus)

        LOGGER.info {
            "${konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.size} batchUider har nå fått sjekket behandlingsstatus. (${
                konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.entries.joinToString(
                    ", ",
                ) { it.key }
            })"
        }
        if (feiledeOverføringer.isNotEmpty()) {
            val feilmeldingSammenslått = feiledeOverføringer.entries.joinToString("\n") { it.value }

            if (skalSendeDagligSlack()) {
                slackService.sendMelding(
                    ":ohno: Sjekk av behandlingsstatus feilet! Miljø: $clientId\n\nFølgende batchUider feilet:\n$feilmeldingSammenslått",
                )
            }
            LOGGER.error { "Det har oppstått feil ved overføring av krav på følgende batchUider med følgende feilmelding:\n $feilmeldingSammenslått" }
            Gauge.builder("behandlingsstatus-feilet-for-antall") { feiledeOverføringer.size }.strongReference(true).register(meterRegistry)
        } else {
            Gauge.builder("behandlingsstatus-feilet-for-antall") { 0 }.strongReference(true).register(meterRegistry)
        }
    }

    // Sørger for at slack melding på behandlingsstatus kun blir sendt en gang om dagen
    private fun skalSendeDagligSlack(): Boolean {
        val now = LocalTime.now()
        val tidFra = LocalTime.parse("07:00:00")
        val tidTil = LocalTime.parse("08:00:00")
        return tidFra.isBefore(now) && tidTil.isAfter(now)
    }
}
