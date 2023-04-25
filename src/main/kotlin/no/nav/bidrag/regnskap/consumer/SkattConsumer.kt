package no.nav.bidrag.regnskap.consumer

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.KotlinLogging
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.dto.behandlingsstatus.BehandlingsstatusResponse
import no.nav.bidrag.regnskap.dto.krav.Kravliste
import no.nav.bidrag.regnskap.dto.påløp.Vedlikeholdsmodus
import no.nav.bidrag.regnskap.maskinporten.MaskinportenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.net.URI

private val LOGGER = KotlinLogging.logger { }
private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

@Service
class SkattConsumer(
    @Value("\${SKATT_URL}") private val skattUrl: String,
    @Value("\${maskinporten.scope}") private val scope: String,
    @Value("\${ELIN_SUBSCRIPTION_KEY}") private val subscriptionKey: String,
    private val restTemplate: RestTemplate,
    private val maskinportenClient: MaskinportenClient
) {

    companion object {
        const val KRAV_PATH = "/api/krav"
        const val LIVENESS_PATH = "/api/liveness"
        const val VEDLIKEHOLDSMODUS_PATH = "/api/vedlikeholdsmodus"
    }

    fun sendKrav(kravliste: Kravliste): ResponseEntity<String> {
        SECURE_LOGGER.info("Overfører krav til skatt: ${objectMapper.writeValueAsString(kravliste)}")
        return try {
            restTemplate.exchange(
                opprettSkattUrl(KRAV_PATH),
                HttpMethod.POST,
                HttpEntity<Kravliste>(kravliste, opprettHttpHeaders()),
                String::class.java
            )
        } catch (e: HttpStatusCodeException) {
            ResponseEntity.status(e.statusCode).body(e.responseBodyAsString)
        }
    }

    fun oppdaterVedlikeholdsmodus(vedlikeholdsmodus: Vedlikeholdsmodus): ResponseEntity<Any> {
        LOGGER.info { "Oppdaterer vedlikeholdsmodud til følgende: $vedlikeholdsmodus" }
        return restTemplate.exchange(
            opprettSkattUrl(VEDLIKEHOLDSMODUS_PATH),
            HttpMethod.POST,
            HttpEntity<Vedlikeholdsmodus>(vedlikeholdsmodus, opprettHttpHeaders()),
            Any::class.java
        )
    }

    fun hentStatusPåVedlikeholdsmodus(): ResponseEntity<Any> {
        LOGGER.info { "Henter status på vedlikeholdsmodus." }
        return try {
            restTemplate.exchange(
                opprettSkattUrl(LIVENESS_PATH),
                HttpMethod.GET,
                HttpEntity<String>(opprettHttpHeaders()),
                Any::class.java
            )
        } catch (e: HttpStatusCodeException) {
            ResponseEntity.status(e.statusCode).body(e.responseBodyAsString)
        }
    }

    fun sjekkBehandlingsstatus(batchUid: String): ResponseEntity<BehandlingsstatusResponse> {
        LOGGER.info { "Henter behandlingsstatus for batchUid: $batchUid" }
        return restTemplate.exchange(
            opprettSkattUrl("$KRAV_PATH/$batchUid"),
            HttpMethod.GET,
            HttpEntity<String>(opprettHttpHeaders()),
            BehandlingsstatusResponse::class.java
        )
    }

    private fun opprettSkattUrl(path: String): URI {
        return URI.create(skattUrl + path)
    }

    private fun opprettHttpHeaders(): HttpHeaders {
        val httpHeaders = HttpHeaders()
        httpHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE)
        httpHeaders.set("Authorization", "Bearer " + hentJwtToken())
        httpHeaders.set("Ocp-Apim-Subscription-Key", subscriptionKey)
        return httpHeaders
    }

    private fun hentJwtToken(): String {
        return maskinportenClient.hentMaskinportenToken(scope).parsedString
    }
}
