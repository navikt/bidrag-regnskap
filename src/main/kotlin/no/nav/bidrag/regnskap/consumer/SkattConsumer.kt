package no.nav.bidrag.regnskap.consumer

import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.regnskap.dto.krav.Krav
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate

@Service
class SkattConsumer(
  @Value("\${SKATT_URL}") skattUrl: String, restTemplate: RestTemplate,
  securityTokenService: SecurityTokenService

) : DefaultConsumer("skatt", skattUrl, restTemplate, securityTokenService) {

  fun sendKrav(krav: Krav): ResponseEntity<String> {
    val skattKravResponse: ResponseEntity<String>
    try {

      skattKravResponse = restTemplate.postForEntity("/ekstern/skatt/api/krav", krav, String::class.java)
    } catch (e: HttpStatusCodeException) {
      return ResponseEntity.status(e.statusCode).body(e.responseBodyAsString)
    }

    return skattKravResponse
  }
}