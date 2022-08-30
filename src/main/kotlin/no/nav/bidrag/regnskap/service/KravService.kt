package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.model.KravRequest
import no.nav.bidrag.regnskap.model.KravResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class KravService(var skattConsumer: SkattConsumer) {

  companion object {
    private val LOGGER = LoggerFactory.getLogger(KravService::class.java)
  }

  fun lagreKrav(kravRequest: KravRequest): ResponseEntity<KravResponse> {
    LOGGER.info("Starter lagring av krav")
    return skattConsumer.lagreKrav(kravRequest)
  }
}