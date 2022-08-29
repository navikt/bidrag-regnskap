package no.nav.bidrag.regnskap.consumer

import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.model.KravRequest
import no.nav.bidrag.regnskap.model.KravResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class SkattConsumer(
  @Value("\${SKATT_URL}") skattUrl: String, restTemplate: RestTemplate, securityTokenService: SecurityTokenService
) : DefaultConsumer("skatt", skattUrl, restTemplate, securityTokenService) {


  companion object {
    private val LOGGER = LoggerFactory.getLogger(SkattConsumer::class.java)
  }

  fun lagreKrav(kravRequest: KravRequest): KravResponse? {
    LOGGER.info("Lagrer krav")
    val hentKravResponse = restTemplate.exchange("/api/krav", HttpMethod.POST, null, KravResponse::class.java)
    SECURE_LOGGER.info("Mottok svar: \n${hentKravResponse.body}")
    return hentKravResponse.body
  }
}