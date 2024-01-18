package no.nav.bidrag.regnskap.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.bidrag.commons.security.maskinporten.MaskinportenClient
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.transport.regnskap.behandlingsstatus.BehandlingsstatusResponse
import no.nav.bidrag.transport.regnskap.krav.Kravliste
import no.nav.bidrag.transport.regnskap.vedlikeholdsmodus.Vedlikeholdsmodus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
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

@Service
class SkattConsumer(
    @Value("\${SKATT_URL}") private val skattUrl: String,
    @Value("\${maskinporten.scope}") private val scope: String,
    @Value("\${ELIN_SUBSCRIPTION_KEY}") private val subscriptionKey: String,
    @Qualifier("regnskap") private val restTemplate: RestTemplate,
    private val maskinportenClient: MaskinportenClient,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry,
) {

    companion object {
        const val KRAV_PATH = "/api/krav"
        const val LIVENESS_PATH = "/api/liveness"
        const val VEDLIKEHOLDSMODUS_PATH = "/api/vedlikeholdsmodus"
    }

    fun sendKrav(kravliste: Kravliste): ResponseEntity<String> {
        SECURE_LOGGER.info("Overfører krav til skatt:\n${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(kravliste)}")
        metrikkerForAntallOversendteKonteringer(kravliste)
        return try {
            restTemplate.exchange(
                opprettSkattUrl(KRAV_PATH),
                HttpMethod.POST,
                HttpEntity<Kravliste>(kravliste, opprettHttpHeaders()),
                String::class.java,
            )
        } catch (e: HttpStatusCodeException) {
            ResponseEntity.status(e.statusCode).body(e.responseBodyAsString)
        }
    }

    private fun metrikkerForAntallOversendteKonteringer(kravliste: Kravliste) {
        kravliste.krav.forEach { krav ->
            krav.konteringer.forEach { kontering ->
                meterRegistry.counter("krav-antall-overfort-grensesnitt", "transaksjonskode", kontering.transaksjonskode.name).increment()
            }
        }
    }

    @CacheEvict(value = ["vedlikeholdsmodus_cache"], allEntries = true)
    fun oppdaterVedlikeholdsmodus(vedlikeholdsmodus: Vedlikeholdsmodus): ResponseEntity<Any> {
        LOGGER.info { "Oppdaterer vedlikeholdsmodud til følgende: $vedlikeholdsmodus" }
        return restTemplate.exchange(
            opprettSkattUrl(VEDLIKEHOLDSMODUS_PATH),
            HttpMethod.POST,
            HttpEntity<Vedlikeholdsmodus>(vedlikeholdsmodus, opprettHttpHeaders()),
            Any::class.java,
        )
    }

    @Cacheable(value = ["vedlikeholdsmodus_cache"], key = "#root.methodName")
    fun hentStatusPåVedlikeholdsmodus(): ResponseEntity<Any> {
        LOGGER.debug { "Henter status på vedlikeholdsmodus." }
        return try {
            restTemplate.exchange(
                opprettSkattUrl(LIVENESS_PATH),
                HttpMethod.GET,
                HttpEntity<String>(opprettHttpHeaders()),
                Any::class.java,
            )
        } catch (e: HttpStatusCodeException) {
            ResponseEntity.status(e.statusCode).body(e.responseBodyAsString)
        }
    }

    fun sjekkBehandlingsstatus(batchUid: String): ResponseEntity<BehandlingsstatusResponse> {
        LOGGER.debug { "Henter behandlingsstatus for batchUid: $batchUid" }
        return restTemplate.exchange(
            opprettSkattUrl("$KRAV_PATH/$batchUid"),
            HttpMethod.GET,
            HttpEntity<String>(opprettHttpHeaders()),
            BehandlingsstatusResponse::class.java,
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
