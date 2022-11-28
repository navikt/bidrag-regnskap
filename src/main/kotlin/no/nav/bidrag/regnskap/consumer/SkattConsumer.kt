package no.nav.bidrag.regnskap.consumer

import no.nav.bidrag.regnskap.dto.krav.Krav
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

@Service
class SkattConsumer(
  @Value("\${SKATT_URL}") private val skattUrl: String,
  @Value("\${maskinporten.scope}") private val scope: String,
  private val restTemplate: RestTemplate,
  private val maskinportenClient: MaskinportenClient
) {

  fun sendKrav(krav: Krav): ResponseEntity<String> {
    val skattKravResponse: ResponseEntity<String>
    try {
      skattKravResponse = restTemplate.exchange(opprettSkattUrl(), HttpMethod.POST, opprettHttpEntity(krav), String::class.java)
    } catch (e: HttpStatusCodeException) {
      return ResponseEntity.status(e.statusCode).body(e.responseBodyAsString)
    }

    return skattKravResponse
  }

  private fun opprettSkattUrl(): URI {
    return URI.create(skattUrl)
  }

  private fun opprettHttpEntity(krav: Krav): HttpEntity<Krav> {
    return HttpEntity<Krav>(krav, opprettHttpHeaders())
  }

  private fun opprettHttpHeaders(): HttpHeaders {
    val httpHeaders = HttpHeaders()
    httpHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE)
    httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE)
    httpHeaders.set("Authorization", "Bearer " + hentJwtToken())
    return httpHeaders
  }

  private fun hentJwtToken(): String {
    return maskinportenClient.hentMaskinportenToken(scope).parsedString
  }
}