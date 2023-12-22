package no.nav.bidrag.regnskap.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.transport.sak.BidragssakDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations

private val LOGGER = KotlinLogging.logger { }

@Service
class SakConsumer(
    @Value("\${SAK_URL}") private val sakUrl: String,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-regnskap") {

    companion object {
        const val SAK_PATH = "/bidrag-sak/sak"
        const val DUMMY_NUMMER = "22222222226"
    }

    @Cacheable(value = ["bidrag-sak_cache"], key = "#sakId")
    fun hentBmFraSak(sakId: String): String {
        return try {
            val responseEntity = restTemplate.getForEntity("$sakUrl$SAK_PATH/$sakId", BidragssakDto::class.java)

            hentFødselsnummerTilBmFraSak(responseEntity) ?: DUMMY_NUMMER
        } catch (e: Exception) {
            LOGGER.error {
                "Noe gikk galt i kommunikasjon med bidrag-sak for sakId: $sakId! \nGjeldende URL mot sak er: ${sakUrl + SAK_PATH} \nFeilmelding: ${e.message}"
            }
            throw e
        }
    }

    private fun hentFødselsnummerTilBmFraSak(responseEntity: ResponseEntity<BidragssakDto>): String? {
        return responseEntity.body?.roller?.find { it.type == Rolletype.BIDRAGSMOTTAKER }?.fødselsnummer?.verdi
    }
}
