package no.nav.bidrag.regnskap.hendelse.påløp

import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.regnskap.service.PersistenceService
import no.nav.bidrag.regnskap.service.PåløpskjøringService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val LOGGER = LoggerFactory.getLogger(PåløpskjøringScheduler::class.java)

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class PåløpskjøringScheduler(
  private val persistenceService: PersistenceService, private val påløpskjøringService: PåløpskjøringService
) {

  @Scheduled(cron = "\${scheduler.påløpkjøring.cron}")
  @SchedulerLock(name = "skedulertPåløpskjøring")
  @Transactional
  fun skedulertPåløpskjøring() {
    LockAssert.assertLocked()

    LOGGER.info("Starter skedulert påløpskjøring..")
    persistenceService.hentIkkeKjørtePåløp().minByOrNull { it.forPeriode }.let {
      if (it != null) {
        if (it.kjøredato.isBefore(LocalDateTime.now())) påløpskjøringService.startPåløpskjøring(it, true)
        else LOGGER.info("Fant ingen påløp som skulle kjøres på dette tidspunkt. " +
            "Neste påløpskjøring er for periode: ${it.forPeriode} som kjøres: ${it.kjøredato}")
      } else LOGGER.error("Det finnes ingen fremtidige planlagte påløp! Påløpsfil kommer ikke til å generes før dette legges inn!")
    }
  }
}