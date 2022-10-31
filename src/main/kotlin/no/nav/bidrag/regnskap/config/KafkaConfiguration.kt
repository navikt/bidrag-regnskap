package no.nav.bidrag.regnskap.config

import no.nav.bidrag.regnskap.hendelse.vedtak.VedtakHendelseListener
import no.nav.bidrag.regnskap.service.VedtakHendelseService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.KafkaListenerErrorHandler
import org.springframework.kafka.listener.ListenerExecutionFailedException
import org.springframework.messaging.Message
import java.util.*

private val LOGGER = LoggerFactory.getLogger(KafkaConfiguration::class.java)

@Configuration
class KafkaConfiguration {

  @Bean
  fun vedtakHendesleListener(vedtakHendelseService: VedtakHendelseService) =
    VedtakHendelseListener(vedtakHendelseService)

  @Bean
  fun vedtakshendelseErrorHandler(): KafkaListenerErrorHandler {
    return KafkaListenerErrorHandler { message: Message<*>, e: ListenerExecutionFailedException ->
      val messagePayload: Any = try {
        message.payload
      } catch (re: RuntimeException) {
        "Unable to read message payload"
      }

      LOGGER.error(
        "Message {} cause error: {} - {} - headers: {}", messagePayload, e.javaClass.simpleName, e.message, message.headers
      )
      Optional.empty<Any>()
    }
  }
}