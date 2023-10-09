package no.nav.bidrag.regnskap.hendelse.schedule.krav

import io.github.oshai.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.regnskap.service.BehandlingsstatusService
import no.nav.bidrag.regnskap.slack.SlackService
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger { }

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class SjekkAvBehandlingsstatusScheduler(
    private val behandlingsstatusService: BehandlingsstatusService,
    private val kravSchedulerUtils: KravSchedulerUtils,
    private val slackService: SlackService,
    private val meterRegistry: MeterRegistry
) {

    companion object {
        var behandlingsstatusAntallFeilet: Number = 0
    }

    @Scheduled(cron = "\${scheduler.behandlingsstatus.cron}")
    @SchedulerLock(name = "skedulertSjekkAvBehandlingsstatus")
    @Transactional
    fun skedulertSjekkAvBehandlingsstatus() {
        LockAssert.assertLocked()
        LOGGER.info { "Starter schedulert sjekk av behandlingsstatus for allerede overførte konteringer." }
        if (kravSchedulerUtils.harAktivtDriftsavvik()) {
            LOGGER.warn { "Det finnes aktive driftsavvik. Starter derfor ikke sjekk av behandlingsstatus." }
            return
        } else if (kravSchedulerUtils.erVedlikeholdsmodusPåslått()) {
            LOGGER.warn { "Vedlikeholdsmodus er påslått! Starter derfor ikke sjekk av behandlingsstatus." }
            return
        }

        val konteringerSomIkkeHarFåttGodkjentBehandlingsstatus = behandlingsstatusService.hentKonteringerMedIkkeGodkjentBehandlingsstatus()

        if (konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.isEmpty()) {
            LOGGER.info { "Det finnes ingen konteringer som ikke har sjekket behandlingsstatus." }
            behandlingsstatusAntallFeilet = 0
            meterRegistry.gauge("behandlingsstatus-feilet-for-antall", behandlingsstatusAntallFeilet)
            return
        }

        val feiledeOverføringer: HashMap<String, String> =
            behandlingsstatusService.hentBehandlingsstatusForIkkeGodkjenteKonteringer(konteringerSomIkkeHarFåttGodkjentBehandlingsstatus)

        LOGGER.info { "${konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.size} batchUider har nå fått sjekket behandlingsstatus. (${konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.entries.joinToString(", ") { it.key }})" }
        if (feiledeOverføringer.isNotEmpty()) {
            val feilmeldingSammenslått = feiledeOverføringer.entries.joinToString("\n") { it.value }

            slackService.sendMelding(":ohno: @channel Sjekk av behandlingsstatus feilet for følgende batchUid:\n $feilmeldingSammenslått")
            LOGGER.error { "Det har oppstått feil ved overføring av krav på følgende batchUider med følgende feilmelding:\n $feilmeldingSammenslått" }
            behandlingsstatusAntallFeilet = feiledeOverføringer.size
            meterRegistry.gauge("behandlingsstatus-feilet-for-antall", behandlingsstatusAntallFeilet)
        } else {
            behandlingsstatusAntallFeilet = 0
            meterRegistry.gauge("behandlingsstatus-feilet-for-antall", behandlingsstatusAntallFeilet)
        }
    }
}
