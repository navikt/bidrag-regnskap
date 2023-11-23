package no.nav.bidrag.regnskap.config

import no.nav.bidrag.regnskap.SECURE_LOGGER
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.RetryListener

class KafkaRetryListener : RetryListener {

    override fun failedDelivery(record: ConsumerRecord<*, *>, exception: Exception, deliveryAttempt: Int) {
        SECURE_LOGGER.error("Håndtering av kafka melding ${record.value()} feilet. Dette er $deliveryAttempt. forsøk", exception)
    }

    override fun recovered(record: ConsumerRecord<*, *>, exception: java.lang.Exception) {
        SECURE_LOGGER.error("Håndtering av kafka melding ${record.value()} er enten suksess eller ignorert pågrunn av ugyldig data", exception)
    }

    override fun recoveryFailed(record: ConsumerRecord<*, *>, original: java.lang.Exception, failure: java.lang.Exception) {
    }
}
