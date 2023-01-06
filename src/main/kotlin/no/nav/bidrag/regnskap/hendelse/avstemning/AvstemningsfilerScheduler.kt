package no.nav.bidrag.regnskap.hendelse.avstemning

import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.regnskap.service.AvstemningService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.LocalDate

private val LOGGER = LoggerFactory.getLogger((AvstemningsfilerScheduler::class.java))

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class AvstemningsfilerScheduler(
  private val avstemningService: AvstemningService
) {

  @Scheduled(cron = "\${scheduler.avstemning.cron}")
  @SchedulerLock(name = "skedulertOpprettelseAvAvstemningsfiler")
  fun skedulertOpprettelseAvAvstemningsfiler() {
    LockAssert.assertLocked()

    val dato = LocalDate.now().minusDays(1)
    LOGGER.debug("Starter schedulert generering av avstemningsfiler for $dato.")

    avstemningService.startAvstemning(dato)
  }

}