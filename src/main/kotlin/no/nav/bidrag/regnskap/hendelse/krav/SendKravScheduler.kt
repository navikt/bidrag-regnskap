package no.nav.bidrag.regnskap.hendelse.krav

import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.regnskap.service.KravService
import no.nav.bidrag.regnskap.service.PersistenceService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

private val LOGGER = LoggerFactory.getLogger(SendKravScheduler::class.java)

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class SendKravScheduler(
  val persistenceService: PersistenceService, val kravService: KravService
) {

  @Scheduled(cron = "\${scheduler.sendkrav.cron}")
  @SchedulerLock(name = "skedulertOverforingAvKrav")
  @Transactional
  fun skedulertOverforingAvKrav() {
    LockAssert.assertLocked()
    LOGGER.info("Starter schedulert overføring av alle konteringer som ikke har blitt overført.")
    if (harAktiveDriftAvvik()) {
      LOGGER.info("Det finnes aktive driftsavvik. Starter derfor ikke overføring av konteringer.")
      return
    } else if (kravService.erVedlikeholdsmodusPåslått()) {
      LOGGER.info("Vedlikeholdsmodus er påslått! Starter derfor ikke overføring av kontering.")
      return
    }

    val oppdragMedIkkeOverforteKonteringer = hentOppdragMedIkkeOverforteKonteringer()

    if (oppdragMedIkkeOverforteKonteringer.isEmpty()) {
      LOGGER.info("Det finnes ingen oppdrag med unsendte konteringer.")
      return
    }

    oppdragMedIkkeOverforteKonteringer.forEach {
      kravService.sendKrav(it, persistenceService.finnSisteOverførtePeriode())
    }
    LOGGER.info("Alle oppdrag(antall: ${oppdragMedIkkeOverforteKonteringer.size}) med unsendte konteringer er nå overført til skatt.")
  }

  private fun harAktiveDriftAvvik(): Boolean {
    return persistenceService.finnesAktivtDriftsavvik()
  }

  private fun hentOppdragMedIkkeOverforteKonteringer() = persistenceService.hentAlleIkkeOverførteKonteringer().flatMap {
    listOf(it.oppdragsperiode?.oppdrag?.oppdragId)
  }.distinct().filterNotNull()
}