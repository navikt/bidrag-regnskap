package no.nav.bidrag.regnskap.hendelse

import no.nav.bidrag.regnskap.service.OverforingTilSkattService
import no.nav.bidrag.regnskap.service.PersistenceService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.LinkedBlockingQueue

private val LOGGER = LoggerFactory.getLogger(SendKonteringerQueue::class.java)

@Component
class SendKonteringerQueue(
  val persistenceService: PersistenceService,
  val overforingTilSkattService: OverforingTilSkattService,
  val konteringPublisher: KonteringPublisher,
) {

  val linkedBlockingQueue = LinkedBlockingQueue<Int>()

  fun leggTil(oppdragId: Int) {
    linkedBlockingQueue.add(oppdragId)
    konteringPublisher.publishKonteringEvent(oppdragId.toString())
  }

  @Transactional
  fun send() {
    while (linkedBlockingQueue.isNotEmpty()) {
      val oppdragId = linkedBlockingQueue.peek()
      LOGGER.info("Fant id $oppdragId i queuen. Starter oversending..")

      val sisteOverfortePeriodeForPalop = persistenceService.finnSisteOverfortePeriode()

      overforingTilSkattService.sendKontering(oppdragId, sisteOverfortePeriodeForPalop)
      // TODO: Feilhåndtering, hvordan går vi frem om det feiler gjenntatte ganger
      linkedBlockingQueue.remove()
    }
  }
}