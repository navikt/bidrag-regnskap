package no.nav.bidrag.regnskap.consumer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.dto.SkattKonteringerRequest
import no.nav.bidrag.regnskap.dto.SkattKonteringerResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException.BadRequest
import org.springframework.web.client.RestTemplate

@Service
class SkattConsumer(
  @Value("\${SKATT_URL}") skattUrl: String,
  restTemplate: RestTemplate,
  securityTokenService: SecurityTokenService
) : DefaultConsumer("skatt", skattUrl, restTemplate, securityTokenService) {

  fun sendKontering(skattKonteringerRequest: SkattKonteringerRequest): ResponseEntity<SkattKonteringerResponse> {
    val skattKonteringerResponse: ResponseEntity<SkattKonteringerResponse>
    try {
      skattKonteringerResponse = restTemplate.postForEntity("/ekstern/skatt/api/krav", skattKonteringerRequest, SkattKonteringerResponse::class.java)
    } catch (e: BadRequest) {
      SECURE_LOGGER.info("En eller flere av konteringene har ikke gått gjennom validering, ${e.message}")
      val objectMapper = jacksonObjectMapper()
      return ResponseEntity(objectMapper.readValue(e.responseBodyAsString, SkattKonteringerResponse::class.java), e.statusCode)
    }
    SECURE_LOGGER.info("Mottok svar fra skatt: \n${skattKonteringerResponse}")
    return skattKonteringerResponse
  }
}