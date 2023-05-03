package no.nav.bidrag.regnskap.maskinporten

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.nimbusds.jwt.SignedJWT
import no.nav.bidrag.regnskap.config.MaskinportenConfig
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers.ofString
import java.net.http.HttpResponse.BodyHandlers.ofString

@Service
class MaskinportenClient(
    private val maskinportenConfig: MaskinportenConfig
) {

    companion object {
        internal const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"
        internal const val CONTENT_TYPE = "application/x-www-form-urlencoded"
    }

    private val maskinportenTokenGenerator = MaskinportenTokenGenerator(maskinportenConfig)

    private val httpClient = HttpClient.newBuilder().build()
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    private val maskinportenTokenCache: LoadingCache<String, MaskinportenTokenCache> =
        Caffeine.newBuilder().build { scope: String ->
            MaskinportenTokenCache(hentNyttJwtToken(scope))
        }

    fun hentMaskinportenToken(scope: String): SignedJWT {
        val cache = maskinportenTokenCache.get(scope) { nyttScope: String ->
            MaskinportenTokenCache(hentNyttJwtToken(nyttScope))
        } ?: error("Feil ved henting eller opprettelse av cached scope for maskinporten-token! Scope: $scope, cache content: $maskinportenTokenCache")
        return cache.run {
            maskinportenToken ?: renew(hentNyttJwtToken(scope))
        }
    }

    private fun hentNyttJwtToken(scope: String): String =
        httpClient.send(opprettMaskinportenTokenRequest(scope), ofString()).run {
            if (statusCode() != 200) throw MaskinportenClientException("Feil ved henting av token: Status: ${statusCode()} , Body: ${body()}")
            mapTilMaskinportenResponseBody(body()).access_token
        }

    private fun mapTilMaskinportenResponseBody(body: String): MaskinportenTokenResponse = try {
        objectMapper.readValue(body)
    } catch (e: Exception) {
        throw MaskinportenClientException("Feil ved deserialisering av response fra maskinporten: $e.message")
    }

    private fun opprettMaskinportenTokenRequest(scope: String): HttpRequest =
        HttpRequest.newBuilder().uri(URI.create(maskinportenConfig.tokenUrl)).header("Content-Type", CONTENT_TYPE)
            .POST(ofString(opprettRequestBody(maskinportenTokenGenerator.genererJwtToken(scope)))).build()

    private fun opprettRequestBody(jwt: String) = "grant_type=$GRANT_TYPE&assertion=$jwt"
}
