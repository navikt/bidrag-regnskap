package no.nav.bidrag.regnskap.hendelse

import no.nav.bidrag.regnskap.service.BehandleHendelseService
import org.springframework.kafka.annotation.KafkaListener

class VedtakHendelseListener(private val behandleHendelseService: BehandleHendelseService) {

  @KafkaListener(groupId = "bidrag-regnskap", topics = ["\${TOPIC_VEDTAK}"], errorHandler = "vedtakshendelseErrorHandler")
  fun lesHendelse(hendelse: String) {
    val vedtakHendelse = behandleHendelseService.mapVedtakHendelse(hendelse)
    behandleHendelseService.behandleHendelse(vedtakHendelse)
  }
}