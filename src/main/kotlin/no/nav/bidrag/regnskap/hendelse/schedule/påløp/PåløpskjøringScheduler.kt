package no.nav.bidrag.regnskap.hendelse.schedule.påløp

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.regnskap.service.PersistenceService
import no.nav.bidrag.regnskap.service.PåløpskjøringService
import no.nav.bidrag.regnskap.slack.SlackService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.LocalDateTime
import java.time.ZoneOffset

private val LOGGER = LoggerFactory.getLogger(PåløpskjøringScheduler::class.java)

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m", defaultLockAtLeastFor = "2m")
class PåløpskjøringScheduler(
    private val persistenceService: PersistenceService,
    private val påløpskjøringService: PåløpskjøringService,
    private val slackService: SlackService,
    private val meterRegistry: MeterRegistry,
    @Value("\${NAIS_CLUSTER_NAME}") private val clusterName: String,
) {

    companion object {
        var nestePåløpskjøring: Number = -1
        var sistePåløpskjøringsdato: Number = -1
    }

    @Scheduled(cron = "\${scheduler.påløpkjøring.cron}")
    @SchedulerLock(name = "skedulertPåløpskjøring")
    fun skedulertPåløpskjøring() {
        LockAssert.assertLocked()

        LOGGER.info("Starter skedulert påløpskjøring..")
        persistenceService.hentIkkeKjørtePåløp().minByOrNull { it.forPeriode }.let {
            if (it != null) {
                if (it.kjøredato.isBefore(LocalDateTime.now())) {
                    påløpskjøringService.startPåløpskjøring(it, true, true)
                } else {
                    LOGGER.info(
                        "Fant ingen påløp som skulle kjøres på dette tidspunkt. Neste påløpskjøring er for periode: ${it.forPeriode} som kjøres: ${it.kjøredato}",
                    )
                }
            } else {
                if (clusterName == "prod-gcp") {
                    slackService.sendMelding("Det finnes ingen fremtidige planlagte påløp! Påløpsfil kommer ikke til å generes før dette legges inn!")
                }
                LOGGER.error("Det finnes ingen fremtidige planlagte påløp! Påløpsfil kommer ikke til å generes før dette legges inn!")
            }
        }

        val påløp = persistenceService.hentPåløp()

        påløp.filter { it.fullførtTidspunkt != null }.maxByOrNull { it.fullførtTidspunkt!! }?.also {
            sistePåløpskjøringsdato = it.fullførtTidspunkt!!.toEpochSecond(ZoneOffset.UTC)
        }
        Gauge.builder("palop-siste-palopskjoring-dato") { sistePåløpskjøringsdato }.strongReference(true).register(meterRegistry)

        påløp.filter { it.fullførtTidspunkt == null }.minByOrNull { it.kjøredato }?.also {
            nestePåløpskjøring = it.kjøredato.toEpochSecond(ZoneOffset.UTC)
        } ?: {
            nestePåløpskjøring = -1
        }
        Gauge.builder("palop-neste-palopskjoring-dato") { nestePåløpskjøring }.strongReference(true).register(meterRegistry)
    }
}
