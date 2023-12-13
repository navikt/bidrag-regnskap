package no.nav.bidrag.regnskap.hendelse.kafka.vedtak

import com.fasterxml.jackson.core.JacksonException
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.service.VedtakshendelseService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.AbstractConsumerSeekAware
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

private val LOGGER = LoggerFactory.getLogger(VedtakshendelseListener::class.java)

@Component
class VedtakshendelseListener(
    private val vedtakshendelseService: VedtakshendelseService,
) : AbstractConsumerSeekAware() {

    companion object {
        var hoppOverNesteMelding = false
        var sisteOffset: Long = -1
    }

    @KafkaListener(groupId = "bidrag-regnskap", topics = ["\${TOPIC_VEDTAK}"])
    fun lesHendelse(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
        acknowledgment: Acknowledgment,
    ) {
        sisteOffset = offset
        try {
            LOGGER.info("Starter behandling av vedtakhendelse med offset: $offset")

            if (hoppOverNesteMelding) {
                LOGGER.info("Hopper over behandling av vedtakhendelse med offset: $offset")
                acknowledgment.acknowledge() // Acknowledge meldingen for å gå videre til neste offset
                hoppOverNesteMelding = false // Nullstill variabelen for å unngå hopping på påfølgende meldinger
                return
            }

            val opprettedeOppdrag = vedtakshendelseService.behandleHendelse(hendelse)

            try {
                LOGGER.info("Starter oversending av oppdrag: $opprettedeOppdrag for vedtakhendelse med offset: $offset.")
                vedtakshendelseService.sendKrav(opprettedeOppdrag)
                LOGGER.info("Ferdig med behandling av vedtakshendelse med offset: $offset")
            } catch (e: Exception) {
                LOGGER.error("Oversending av krav feilet for oppdrag: $opprettedeOppdrag med offset: $offset! Feilmelding: $e")
            } finally {
                // Markerer vedtaket som opprettet for Kafka slik at om oversending feiler vil neste vedtak leses inn og prosesseres
                acknowledgment.acknowledge()
            }
        } catch (e: JacksonException) {
            LOGGER.error(
                "Mapping av hendelse feilet for kafkamelding med offsett: $offset, topic: $topic, recieved_partition: $partition, groupId: $groupId!" +
                    "\nSe secure log for mer informasjon.",
            )
            SECURE_LOGGER.error(
                "Mapping av hendelse feilet for kafkamelding med offsett: $offset, topic: $topic, recieved_partition: $partition, groupId: $groupId!! " +
                    "\nFeil: $e \n\nHendelse: $hendelse",
            )
            throw e
        }
    }

    fun hoppOverNesteMelding() {
        hoppOverNesteMelding = true
    }

    fun hoppOverAlleMeldinger() {
        this.seekToEnd()
    }

    fun hentSisteLesteHendelse(): Long {
        return sisteOffset
    }
}
