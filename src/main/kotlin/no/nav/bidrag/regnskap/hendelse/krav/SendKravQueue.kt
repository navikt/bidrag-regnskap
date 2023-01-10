package no.nav.bidrag.regnskap.hendelse.krav

import no.nav.bidrag.regnskap.service.KravService
import no.nav.bidrag.regnskap.service.PersistenceService
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import java.util.concurrent.LinkedBlockingQueue

private val LOGGER = LoggerFactory.getLogger(SendKravQueue::class.java)

@Component
class SendKravQueue(
  val persistenceService: PersistenceService,
  val kravService: KravService,
  val kravPublisher: KravPublisher,
) {

  val linkedBlockingQueue = LinkedBlockingQueue<Int>()

  fun leggTil(oppdragId: Int) {
    linkedBlockingQueue.add(oppdragId)
    kravPublisher.publishKravEvent(oppdragId.toString())
  }

  @Transactional(noRollbackFor = [HttpClientErrorException::class, HttpServerErrorException::class, JwtTokenUnauthorizedException::class])
  fun send() {
    if(harAktiveDriftAvvik()) {
      LOGGER.info("Det finnes aktive driftsavvik! Starter derfor ikke overføring av kontering.")
      return
    } else if (kravService.erVedlikeholdsmodusPåslått()) {
      LOGGER.info("Vedlikeholdsmodus er påslått! Starter derfor ikke overføring av kontering.")
      return
    }
    while (linkedBlockingQueue.isNotEmpty()) {
      val oppdragId = linkedBlockingQueue.poll()
      LOGGER.debug("Fant id $oppdragId i queuen. Starter oversending av krav..")

      val sisteOverfortePeriodeForPalop = persistenceService.finnSisteOverførtePeriode()

      kravService.sendKrav(oppdragId, sisteOverfortePeriodeForPalop)
    }
  }

  private fun harAktiveDriftAvvik(): Boolean {
    return persistenceService.harAktivtDriftsavvik()
  }
}