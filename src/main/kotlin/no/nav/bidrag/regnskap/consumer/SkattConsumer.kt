package no.nav.bidrag.regnskap.consumer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.model.KravRequest
import no.nav.bidrag.regnskap.model.KravResponse
import org.slf4j.LoggerFactory
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


  companion object {
    private val LOGGER = LoggerFactory.getLogger(SkattConsumer::class.java)
  }

  fun lagreKrav(kravRequest: KravRequest): ResponseEntity<KravResponse> {
    LOGGER.info("Lagrer krav")
    val kravResponse: ResponseEntity<KravResponse>
    try {
      kravResponse = restTemplate.postForEntity("/ekstern/skatt/api/krav", kravRequest, KravResponse::class.java)
    } catch (e: BadRequest) {
      SECURE_LOGGER.info("En eller flere av konteringene har ikke gått gjennom validering, ${e.message}")
      val objectMapper = jacksonObjectMapper()
      return ResponseEntity(objectMapper.readValue(e.responseBodyAsString, KravResponse::class.java), e.statusCode)
    }
    SECURE_LOGGER.info("Mottok svar: \n${kravResponse}")
    return kravResponse
  }
}