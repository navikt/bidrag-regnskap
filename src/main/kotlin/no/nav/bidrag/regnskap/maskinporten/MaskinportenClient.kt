package no.nav.bidrag.regnskap.maskinporten

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
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

  private var maskinportenTokenCache: MutableMap<List<String>, MaskinportenTokenCache> = HashMap()
  private val maskinportenTokenGenerator = MaskinportenTokenGenerator(maskinportenConfig)

  private val httpClient = HttpClient.newBuilder().build()
  private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

  fun hentMaskinportenToken(vararg scope: String): SignedJWT =
    maskinportenTokenCache.getOrPut(scope.distinct().sorted()) {
      MaskinportenTokenCache(hentNyttJwtToken(*scope))
    }.run {
      maskinportenToken ?: renew(hentNyttJwtToken(*scope))
    }

  private fun hentNyttJwtToken(vararg scope: String): String =
    httpClient.send(opprettMaskinportenTokenRequest(*scope), ofString()).run {
      if (statusCode() != 200) throw MaskinportenClientException("Feil ved henting av token: Status: ${statusCode()} , Body: ${body()}")
      mapTilMaskinportenResponseBody(body()).access_token
    }

  private fun mapTilMaskinportenResponseBody(body: String): MaskinportenTokenResponse = try {
    objectMapper.readValue(body)
  } catch (e: Exception) {
    throw MaskinportenClientException("Feil ved deserialisering av response fra maskinporten: $e.message")
  }

  private fun opprettMaskinportenTokenRequest(vararg scope: String): HttpRequest =
    HttpRequest.newBuilder().uri(URI.create(maskinportenConfig.tokenUrl)).header("Content-Type", CONTENT_TYPE)
      .POST(ofString(opprettRequestBody(maskinportenTokenGenerator.genererJwtToken(scope.distinct())))).build()

  private fun opprettRequestBody(jwt: String) = "grant_type=$GRANT_TYPE&assertion=$jwt"

}