package no.nav.bidrag.regnskap.config

import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.metrics.web.client.ObservationRestTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.client.observation.ClientRequestObservationConvention
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.web.client.RestTemplate

@Configuration
@EnableSecurityConfiguration
@Import(RestOperationsAzure::class)
class RestTemplateConfiguration {

    @Bean
    fun kotlinModule(): KotlinModule = KotlinModule.Builder().build()

    @Bean("regnskap")
    @Scope("prototype")
    fun baseRestTemplate(
        @Value("\${NAIS_APP_NAME}") naisAppName: String,
        observationRestTemplateCustomizer: ObservationRestTemplateCustomizer,
    ): RestTemplate {
        val restTemplate = HttpHeaderRestTemplate()
        restTemplate.requestFactory = HttpComponentsClientHttpRequestFactory()
        restTemplate.withDefaultHeaders()
        restTemplate.addHeaderGenerator("Nav-Callid") { CorrelationId.fetchCorrelationIdForThread() }
        restTemplate.addHeaderGenerator("Nav-Consumer-Id") { naisAppName }
        observationRestTemplateCustomizer.customize(restTemplate)
        return restTemplate
    }

    @Bean
    fun clientRequestObservationConvention(): ClientRequestObservationConvention = DefaultClientRequestObservationConvention()
}
