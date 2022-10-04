import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import no.nav.bidrag.regnskap.dto.SkattFeiletKonteringerResponse
import org.junit.Assert
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component


@Component
class StubUtils {
    companion object {
            fun aClosedJsonResponse(): ResponseDefinitionBuilder {
                return aResponse()
                    .withHeader(HttpHeaders.CONNECTION, "close")
                    .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            }
    }

    fun stubKravResponse(skattFeiletKonteringerResponse: SkattFeiletKonteringerResponse?, httpStatus: HttpStatus) {
        try {
            stubFor(
                post(anyUrl()).willReturn(
                    aClosedJsonResponse()
                        .withStatus(httpStatus.value())
                        .withBody(ObjectMapper().registerModule(JavaTimeModule()).writeValueAsString(skattFeiletKonteringerResponse))
                )
            )
        } catch (e: JsonProcessingException) {
            Assert.fail(e.message)
        }
    }
}