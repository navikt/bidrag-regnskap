package no.nav.bidrag.regnskap.hendelse

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

private val LOGGER = LoggerFactory.getLogger(KonteringPublisher::class.java)

@Component
class KonteringPublisher(
  val eventPublisher: ApplicationEventPublisher
) {

  fun publishKonteringEvent(oppdragId: String) {
    LOGGER.info("Mottok event for sending av kontering for oppdrag med ID: $oppdragId")
    eventPublisher.publishEvent(oppdragId)
  }
}