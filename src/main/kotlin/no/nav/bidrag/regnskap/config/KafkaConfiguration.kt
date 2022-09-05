package no.nav.bidrag.regnskap.config

import no.nav.bidrag.regnskap.hendelse.VedtakHendelseListener
import no.nav.bidrag.regnskap.service.BehandleHendelseService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.listener.KafkaListenerErrorHandler
import org.springframework.kafka.listener.ListenerExecutionFailedException
import org.springframework.messaging.Message
import java.util.*


const val LIVE_PROFILE = "live"
private val LOGGER = LoggerFactory.getLogger(KafkaConfiguration::class.java)

@Configuration
@Profile(LIVE_PROFILE)
class KafkaConfiguration {

  @Bean
  fun vedtakHendesleListener(behandleHendelseService: BehandleHendelseService) = VedtakHendelseListener(behandleHendelseService)

  @Bean
  fun vedtakshendelseErrorHandler(): KafkaListenerErrorHandler {
    return KafkaListenerErrorHandler { message: Message<*>, e: ListenerExecutionFailedException ->
      val messagePayload: Any = try {
        message.payload
      } catch (re: RuntimeException) {
        "Unable to read message payload"
      }

      LOGGER.error("Message {} cause error: {} - {} - headers: {}", messagePayload, e.javaClass.simpleName, e.message, message.headers)
      Optional.empty<Any>()
    }
  }
}