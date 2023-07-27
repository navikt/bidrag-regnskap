package no.nav.bidrag.regnskap.hendelse.schedule.påløp

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

private val LOGGER = LoggerFactory.getLogger(PåløpskjøringScheduler::class.java)

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m", defaultLockAtLeastFor = "2m")
class PåløpskjøringScheduler(
    private val persistenceService: PersistenceService,
    private val påløpskjøringService: PåløpskjøringService,
    private val slackService: SlackService,
    @Value("\${NAIS_CLUSTER_NAME}") private val clusterName: String
) {

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
                    LOGGER.info("Fant ingen påløp som skulle kjøres på dette tidspunkt. Neste påløpskjøring er for periode: ${it.forPeriode} som kjøres: ${it.kjøredato}")
                }
            } else {
                if (clusterName == "prod-gcp") {
                    slackService.sendMelding("Det finnes ingen fremtidige planlagte påløp! Påløpsfil kommer ikke til å generes før dette legges inn!")
                }
                LOGGER.error("Det finnes ingen fremtidige planlagte påløp! Påløpsfil kommer ikke til å generes før dette legges inn!")
            }
        }
    }
}
