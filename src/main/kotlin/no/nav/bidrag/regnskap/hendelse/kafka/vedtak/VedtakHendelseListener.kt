package no.nav.bidrag.regnskap.hendelse.kafka.vedtak

import com.fasterxml.jackson.core.JacksonException
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.service.VedtakHendelseService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener

private val LOGGER = LoggerFactory.getLogger(VedtakHendelseListener::class.java)

class VedtakHendelseListener(
  private val vedtakHendelseService: VedtakHendelseService
) {

  @KafkaListener(
    groupId = "bidrag-regnskap", topics = ["\${TOPIC_VEDTAK}"], errorHandler = "vedtakshendelseErrorHandler"
  )
  fun lesHendelse(hendelse: String) {
    try {
      vedtakHendelseService.behandleHendelse(hendelse)
    } catch (e: JacksonException) {
      LOGGER.error("Mapping av hendelse feilet! Se secure log for mer informasjon.")
      SECURE_LOGGER.error("Mapping av hendelse feilet! Hendelse: $hendelse Feil: $e")
    }
  }
}