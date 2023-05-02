package no.nav.bidrag.regnskap.consumer

import io.github.oshai.KotlinLogging
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.regnskap.dto.sak.BidragSak
import no.nav.bidrag.regnskap.dto.sak.enumer.Rolletype
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations

private val LOGGER = KotlinLogging.logger { }

@Service
class SakConsumer(
    @Value("\${SAK_URL}") private val sakUrl: String,
    @Qualifier("azure") private val restTemplate: RestOperations
) : AbstractRestClient(restTemplate, "bidrag-aktoerregister-regnskap") {

    companion object {
        const val SAK_PATH = "/bidrag-sak/sak"
        const val DUMMY_NUMMER = "22222222226"
    }

    fun hentBmFraSak(sakId: String): String {
        return try {
            val responseEntity = restTemplate.getForEntity("$sakUrl$SAK_PATH/$sakId", BidragSak::class.java)

            hentFødselsnummerTilBmFraSak(responseEntity) ?: DUMMY_NUMMER
        } catch (e: Exception) {
            LOGGER.error("Noe gikk feil i kommunikasjon med bidrag-sak for sakId: $sakId! \nGjeldende URL mot sak er: ${sakUrl + SAK_PATH} \nFeilmelding: ${e.message}")
            throw e
        }
    }

    private fun hentFødselsnummerTilBmFraSak(responseEntity: ResponseEntity<BidragSak>): String? {
        return responseEntity.body?.roller?.find { it.type == Rolletype.BM }?.fødselsnummer
    }
}
