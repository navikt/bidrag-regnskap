package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.model.KravRequest
import no.nav.bidrag.regnskap.model.KravResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(KravService::class.java)

@Service
class KravService(var skattConsumer: SkattConsumer) {

  fun lagreKrav(kravRequest: KravRequest): ResponseEntity<KravResponse> {
    LOGGER.info("Starter lagring av krav")
    return skattConsumer.lagreKrav(kravRequest)
  }
}