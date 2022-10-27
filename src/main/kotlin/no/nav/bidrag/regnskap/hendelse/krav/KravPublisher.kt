package no.nav.bidrag.regnskap.hendelse.krav

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

private val LOGGER = LoggerFactory.getLogger(KravPublisher::class.java)

@Component
class KravPublisher(
  val eventPublisher: ApplicationEventPublisher
) {

  fun publishKravEvent(oppdragId: String) {
    LOGGER.debug("Mottok event for sending av krav med kontering for oppdrag med ID: $oppdragId")
    eventPublisher.publishEvent(oppdragId)
  }
}