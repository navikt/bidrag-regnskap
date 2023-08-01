package no.nav.bidrag.regnskap.hendelse.kafka.pdl

import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.pdl.aktor.v2.Aktor
import no.nav.bidrag.regnskap.pdl.aktor.v2.Type
import no.nav.bidrag.regnskap.service.AktorhendelseService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class AktorhendelserListener(
    private val aktorhendelseService: AktorhendelseService
) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AktorhendelserListener::class.java)
    }

    @KafkaListener(
        groupId = "\${AKTOR-V2_GROUP_ID}",
        topics = ["\${TOPIC_PDL_AKTOR_V2}"],
        properties = ["auto.offset.reset:latest"],
    )
    fun lesHendelse(
        consumerRecord: ConsumerRecord<String, Aktor?>,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
        acknowledgment: Acknowledgment
    ) {
        try {
            val aktør = consumerRecord.value()

            LOGGER.info("Behandler aktorhendelse med offset: $offset i consumergroup: $groupId for topic: $topic")
            SECURE_LOGGER.info("Behandler aktorhendelse: $consumerRecord")

            aktør?.identifikatorer?.singleOrNull { ident ->
                ident.type == Type.FOLKEREGISTERIDENT && ident.gjeldende
            }?.also { folkeregisterident ->
                aktorhendelseService.behandleAktoerHendelse(folkeregisterident.idnummer.toString())
            }
            acknowledgment.acknowledge()
        } catch (e: RuntimeException) {
            LOGGER.warn(
                "Feil i prosessering av ident-hendelser med offsett: $offset, topic: $topic, recieved_partition: $partition, groupId: $groupId",
                e
            )
            SECURE_LOGGER.warn(
                "Feil i prosessering av ident-hendelser med offsett: $offset, topic: $topic, recieved_partition: $partition, groupId: $groupId." +
                        "\n$consumerRecord", e
            )
            throw RuntimeException("Feil i prosessering av ident-hendelser")
        }
    }
}
