package no.nav.bidrag.regnskap.hendelse.krav

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

private val LOGGER = LoggerFactory.getLogger(KravListener::class.java)

@Component
class KravListener(
  val sendKravQueue: SendKravQueue
) {

  @Async
  @EventListener
  fun sendKravEvent(oppdragId: String) {
    LOGGER.debug("Starter oversending av krav med konteringer for oppdrag med ID: $oppdragId")
    Thread.sleep(500)
    sendKravQueue.send()
  }
}