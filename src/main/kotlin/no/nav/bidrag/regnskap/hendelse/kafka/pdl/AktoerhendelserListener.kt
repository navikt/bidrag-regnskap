package no.nav.bidrag.regnskap.hendelse.kafka.pdl

import io.github.oshai.KotlinLogging
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.service.AktoerhendelseService
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class AktoerhendelserListener(
    private val aktoerhendelseService: AktoerhendelseService
) {

    @KafkaListener(groupId = "\${AKTOR-V2_GROUP_ID}", topics = ["\${TOPIC_PDL_AKTOR_V2}"])
    fun lesHendelse(consumerRecord: ConsumerRecord<String, Aktor?>) {
        try {
            val aktør = consumerRecord.value()

            SECURE_LOGGER.info("Behandler aktorhendelse: $consumerRecord")

            aktør?.identifikatorer?.singleOrNull { ident ->
                ident.type == Type.FOLKEREGISTERIDENT && ident.gjeldende
            }?.also { folkeregisterident ->
                aktoerhendelseService.behandleAktoerHendelse(folkeregisterident.idnummer.toString())
            }
        } catch (e: RuntimeException) {
            LOGGER.warn("Feil i prosessering av ident-hendelser", e)
            SECURE_LOGGER.warn("Feil i prosessering av ident-hendelser $consumerRecord", e)
            throw RuntimeException("Feil i prosessering av ident-hendelser")
        }
    }
}
