package no.nav.bidrag.regnskap.hendelse.schedule.avstemning

import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.regnskap.service.AvstemmingService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.LocalDate

private val LOGGER = LoggerFactory.getLogger((AvstemmingsfilerScheduler::class.java))

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class AvstemmingsfilerScheduler(
    private val avstemmingService: AvstemmingService,
) {

    @Scheduled(cron = "\${scheduler.avstemning.cron}")
    @SchedulerLock(name = "skedulertOpprettelseAvAvstemmingsfiler")
    fun skedulertOpprettelseAvAvstemmingsfiler() {
        LockAssert.assertLocked()

        val dato = LocalDate.now().minusDays(1)
        LOGGER.info("Starter schedulert generering av avstemmingsfiler for $dato.")

        avstemmingService.startAvstemming(dato)
    }
}
