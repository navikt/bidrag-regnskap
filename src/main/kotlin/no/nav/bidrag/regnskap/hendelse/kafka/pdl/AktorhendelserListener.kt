package no.nav.bidrag.regnskap.hendelse.kafka.pdl

import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.pdl.aktor.v2.Aktor
import no.nav.bidrag.regnskap.pdl.aktor.v2.Type
import no.nav.bidrag.regnskap.service.AktorhendelseService
import org.apache.avro.generic.GenericData
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
        groupId = "\${AKTOR_V2_GROUP_ID}",
        topics = ["\${TOPIC_PDL_AKTOR_V2}"],
        properties = ["auto.offset.reset:latest"]
    )
    fun lesHendelse(
        consumerRecord: ConsumerRecord<String, GenericData.Record?>,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
        acknowledgment: Acknowledgment
    ) {
        try {
            LOGGER.info("Behandler aktorhendelse med offset: $offset i consumergroup: $groupId for topic: $topic")

            val genericRecord = GenericData.Record(Aktor.getClassSchema())
            val identifikatorerField = genericRecord.get("identifikatorer")

            if (identifikatorerField is List<*>) {
                val aktor = identifikatorerField.filterIsInstance<GenericData.Record>()

                for (identifikator in aktor) {
                    if (identifikator.get("gjeldende") as Boolean && identifikator.get("type") as Type == Type.FOLKEREGISTERIDENT) {
                        val ident = identifikator.get("idnummer") as String
                        aktorhendelseService.behandleAktoerHendelse(ident)
                        SECURE_LOGGER.info("Oppdatert ident: $ident")
                    }
                }
            }
            acknowledgment.acknowledge()
            LOGGER.info("Behandlet hendelse med offset: $offset")
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
