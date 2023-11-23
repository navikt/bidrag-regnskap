package no.nav.bidrag.regnskap.config

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.micrometer.core.instrument.MeterRegistry
import no.nav.bidrag.regnskap.SECURE_LOGGER
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.MicrometerConsumerListener
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries
import org.springframework.util.backoff.ExponentialBackOff
import java.time.Duration

private val LOGGER = LoggerFactory.getLogger(KafkaConfiguration::class.java)

@Configuration
class KafkaConfiguration(
    val environment: Environment,
    val meterRegistry: MeterRegistry,
) {

    @Bean
    fun defaultErrorHandler(@Value("\${KAFKA_MAX_RETRY:-1}") maxRetry: Int): DefaultErrorHandler {
        // Max retry should not be set in production
        val backoffPolicy = if (maxRetry == -1) ExponentialBackOff() else ExponentialBackOffWithMaxRetries(maxRetry)
        backoffPolicy.multiplier = 2.0
        backoffPolicy.maxInterval = 1800000L // 30 mins
        LOGGER.info(
            "Initializing Kafka errorhandler with backoffpolicy {}, maxRetry={}",
            backoffPolicy,
            maxRetry,
        )
        val errorHandler = DefaultErrorHandler({ rec, e ->
            val key = rec.key()
            val value = rec.value()
            val offset = rec.offset()
            val topic = rec.topic()
            val partition = rec.partition()
            SECURE_LOGGER.error(
                "Kafka melding med nøkkel $key, partition $partition og topic $topic feilet på offset $offset. Melding som feilet: $value",
                e,
            )
        }, backoffPolicy)
        errorHandler.setRetryListeners(KafkaRetryListener())
        return errorHandler
    }

    @Bean
    fun kafkaAktorV2HendelseContainerFactory(defaultErrorHandler: DefaultErrorHandler): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.consumerFactory = DefaultKafkaConsumerFactory<String?, String?>(consumerConfigsLatestAvro()).apply {
            addListener(MicrometerConsumerListener(meterRegistry))
        }
        factory.setContainerCustomizer {
            it.containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(10))
            it.containerProperties.isMicrometerEnabled = true
            it.containerProperties.isObservationEnabled = true
        }
        factory.setCommonErrorHandler(defaultErrorHandler)
        return factory
    }

    private fun consumerConfigsLatestAvro(): Map<String, Any> {
        val kafkaBrokers = System.getenv("KAFKA_BROKERS") ?: "http://localhost:9092"
        val schemaRegisty = System.getenv("KAFKA_SCHEMA_REGISTRY") ?: "http://localhost:9093"
        val schemaRegistryUser = System.getenv("KAFKA_SCHEMA_REGISTRY_USER") ?: "mangler i pod"
        val schemaRegistryPassword = System.getenv("KAFKA_SCHEMA_REGISTRY_PASSWORD") ?: "mangler i pod"
        val consumerConfigs = mutableMapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
            "schema.registry.url" to schemaRegisty,
            "basic.auth.credentials.source" to "USER_INFO",
            "basic.auth.user.info" to "$schemaRegistryUser:$schemaRegistryPassword",
            "specific.avro.reader" to true,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
            ConsumerConfig.CLIENT_ID_CONFIG to "consumer-bidrag-regnskap",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
        )

        if (environment.activeProfiles.none { it.contains("local") || it.contains("h2") || it.contains("test") }) {
            return consumerConfigs + securityConfig()
        }
        return consumerConfigs
    }

    private fun securityConfig(): Map<String, Any> {
        val kafkaTruststorePath = System.getenv("KAFKA_TRUSTSTORE_PATH")
        val kafkaCredstorePassword = System.getenv("KAFKA_CREDSTORE_PASSWORD")
        val kafkaKeystorePath = System.getenv("KAFKA_KEYSTORE_PATH")
        return mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
            // Disable server host name verification
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to kafkaTruststorePath,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to kafkaKeystorePath,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to kafkaCredstorePassword,
        )
    }
}
