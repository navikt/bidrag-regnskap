package no.nav.bidrag.regnskap.consumer

import no.nav.bidrag.regnskap.dto.sak.BidragSak
import no.nav.bidrag.regnskap.dto.sak.enumer.Rolletype
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Service
class SakConsumer(
  @Value("\${SAK_URL}") private val sakUrl: String,
  private val restTemplate: RestTemplate
) {

  companion object {
    const val SAK_PATH = "/bidrag-sak/sak"
    const val DUMMY_NUMMER = "22222222226"
  }

  fun hentBmFraSak(sakId: String): String {
    val headers = HttpHeaders().apply { set("header", sakId) }
    val requestEntity = HttpEntity("parameters", headers)

    return try {
      val responseEntity = restTemplate.exchange(
        sakUrl + SAK_PATH,
        HttpMethod.GET,
        requestEntity,
        BidragSak::class.java
      )
      hentFødselsnummerTilBmFraSak(responseEntity) ?: DUMMY_NUMMER
    } catch (e: HttpClientErrorException) {
      DUMMY_NUMMER
    }
  }

  private fun hentFødselsnummerTilBmFraSak(responseEntity: ResponseEntity<BidragSak>): String? {
    return responseEntity.body?.roller?.find { it.type == Rolletype.BM }?.fødselsnummer
  }
}