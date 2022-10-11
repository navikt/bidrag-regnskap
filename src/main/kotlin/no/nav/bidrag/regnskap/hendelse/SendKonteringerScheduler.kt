package no.nav.bidrag.regnskap.hendelse

import no.nav.bidrag.regnskap.service.OverforingTilSkattService
import no.nav.bidrag.regnskap.service.PersistenceService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

private val LOGGER = LoggerFactory.getLogger(SendKonteringerScheduler::class.java)

@Configuration
@EnableScheduling
class SendKonteringerScheduler(
  val persistenceService: PersistenceService,
  val overforingTilSkattService: OverforingTilSkattService
) {

  @Scheduled(cron = "\${scheduler.interval.cron}")
  @Transactional
  fun skedulertOverforingAvKonteringer() {
    if(harAktiveDriftAvvik()) {
      LOGGER.info("Det finnes aktive driftsavvik. Starter derfor ikke batchen.")
      return
    }

    LOGGER.info("Henter alle oppdrag med konteringer som ikke har blitt overført.")
    val oppdragMedIkkeOverforteKonteringer = hentOppdragMedIkkeOverforteKonteringer()

    if(oppdragMedIkkeOverforteKonteringer.isEmpty()) {
      LOGGER.info("Det finnes ingen oppdrag med unsendte konteringer.")
      return
    }

    oppdragMedIkkeOverforteKonteringer.forEach {
      overforingTilSkattService.sendKontering(it, persistenceService.finnSisteOverfortePeriode())
    }
    LOGGER.info("Alle oppdrag med unsendte konteringer er nå overført til skatt.")
  }

  private fun harAktiveDriftAvvik(): Boolean {
    return false // TODO()
  }

  private fun hentOppdragMedIkkeOverforteKonteringer() = persistenceService.hentAlleIkkeOverforteKonteringer().flatMap {
    listOf(it.oppdragsperiode?.oppdrag?.oppdragId)
  }.distinct().filterNotNull()
}