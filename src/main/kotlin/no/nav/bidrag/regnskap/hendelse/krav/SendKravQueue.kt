package no.nav.bidrag.regnskap.hendelse.krav

import no.nav.bidrag.regnskap.service.KravService
import no.nav.bidrag.regnskap.service.PersistenceService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
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

  @Transactional
  fun send() {
    while (linkedBlockingQueue.isNotEmpty()) {
      val oppdragId = linkedBlockingQueue.peek()
      LOGGER.debug("Fant id $oppdragId i queuen. Starter oversending av krav..")

      val sisteOverfortePeriodeForPalop = persistenceService.finnSisteOverfortePeriode()

      kravService.sendKrav(oppdragId, sisteOverfortePeriodeForPalop)
      // TODO(): Feilhåndtering, hvordan går vi frem om det feiler gjenntatte ganger
      linkedBlockingQueue.remove()
    }
  }
}