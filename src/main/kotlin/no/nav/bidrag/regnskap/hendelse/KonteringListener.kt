package no.nav.bidrag.regnskap.hendelse

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

private val LOGGER = LoggerFactory.getLogger(KonteringListener::class.java)

@Component
class KonteringListener(
  val sendKonteringerQueue: SendKonteringerQueue
) {

  @Async
  @EventListener
  fun sendKonteringEvent(oppdragId: String) {
    LOGGER.info("Starter oversending av konteringer for oppdrag med ID: $oppdragId")
    sendKonteringerQueue.send()
  }
}