package no.nav.bidrag.regnskap.config

import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.hendelse.kafka.vedtak.VedtakshendelseListener
import no.nav.bidrag.regnskap.service.VedtakshendelseService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries
import org.springframework.util.backoff.ExponentialBackOff

private val LOGGER = LoggerFactory.getLogger(KafkaConfiguration::class.java)

@Configuration
class KafkaConfiguration {

    @Bean
    fun vedtakHendesleListener(vedtakshendelseService: VedtakshendelseService) = VedtakshendelseListener(vedtakshendelseService)

    @Bean
    fun defaultErrorHandler(@Value("\${KAFKA_MAX_RETRY:-1}") maxRetry: Int): DefaultErrorHandler {
        // Max retry should not be set in production
        val backoffPolicy = if (maxRetry == -1) ExponentialBackOff() else ExponentialBackOffWithMaxRetries(maxRetry)
        backoffPolicy.multiplier = 2.0
        backoffPolicy.maxInterval = 1800000L // 30 mins
        LOGGER.info(
            "Initializing Kafka errorhandler with backoffpolicy {}, maxRetry={}",
            backoffPolicy,
            maxRetry
        )
        val errorHandler = DefaultErrorHandler({ rec, e ->
            val key = rec.key()
            val value = rec.value()
            val offset = rec.offset()
            val topic = rec.topic()
            val partition = rec.partition()
            SECURE_LOGGER.error(
                "Kafka melding med nøkkel $key, partition $partition og topic $topic feilet på offset $offset. Melding som feilet: $value",
                e
            )
        }, backoffPolicy)
        errorHandler.setRetryListeners(KafkaRetryListener())
        return errorHandler
    }
}
