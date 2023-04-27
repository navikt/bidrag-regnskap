package no.nav.bidrag.regnskap.hendelse.kafka.vedtak

import com.fasterxml.jackson.core.JacksonException
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.service.VedtakshendelseService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header

private val LOGGER = LoggerFactory.getLogger(VedtakshendelseListener::class.java)

class VedtakshendelseListener(
    private val vedtakshendelseService: VedtakshendelseService
) {

    @KafkaListener(groupId = "bidrag-regnskap", topics = ["\${TOPIC_VEDTAK}"])
    fun lesHendelse(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String
    ) {
        try {
            vedtakshendelseService.behandleHendelse(hendelse)
        } catch (e: JacksonException) {
            LOGGER.error(
                "Mapping av hendelse feilet for kafkamelding med offsett: $offset, topic: $topic, recieved_partition: $partition, groupId: $groupId!" +
                    "\nSe secure log for mer informasjon."
            )
            SECURE_LOGGER.error(
                "Mapping av hendelse feilet for kafkamelding med offsett: $offset, topic: $topic, recieved_partition: $partition, groupId: $groupId!! " +
                    "\nFeil: $e \n\nHendelse: $hendelse"
            )
        }
    }
}
