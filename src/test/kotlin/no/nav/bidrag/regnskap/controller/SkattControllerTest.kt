package no.nav.bidrag.regnskap.controller

import StubUtils
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.regnskap.BidragRegnskapLocal
import no.nav.bidrag.regnskap.model.KonteringId
import no.nav.bidrag.regnskap.model.Konteringsfeil
import no.nav.bidrag.regnskap.model.KravRequest
import no.nav.bidrag.regnskap.model.KravResponse
import no.nav.bidrag.regnskap.model.Transaksjonskode
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.time.YearMonth

@ActiveProfiles("test")
@SpringBootTest(
  classes = [BidragRegnskapLocal::class, StubUtils::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
class SkattControllerTest {

  @LocalServerPort
  private val port = 0

  @Autowired
  lateinit var stubUtils: StubUtils

  @Autowired
  lateinit var httpHeadTestRestTemplate: HttpHeaderTestRestTemplate

  @BeforeEach
  fun init() {
    WireMock.reset()
    WireMock.resetToDefault()
  }

  @Test
  fun `Skal lagre krav konteringer hos Skatteetaten`() {
    stubUtils.stubKravResponse(KravResponse(emptyList()), HttpStatus.OK)

    val response = httpHeadTestRestTemplate.exchange(
      "http://localhost:$port/api/krav", HttpMethod.POST, HttpEntity(KravRequest(emptyList())), KravResponse::class.java
    )

    Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
  }

  @Test
  fun `Skal ta i mot feil i konteringer fra Skatteetaten`() {
    stubUtils.stubKravResponse(
      KravResponse(
        listOf(
          Konteringsfeil(
            "TOLKNING", "Tolkning feilet i Elin.", KonteringId(
              Transaksjonskode.B1, YearMonth.parse("2022-01"), "123456789"
            )
          )
        )
      ),
    HttpStatus.BAD_REQUEST)

    val response = httpHeadTestRestTemplate.exchange(
      "http://localhost:$port/api/krav", HttpMethod.POST, HttpEntity(KravRequest(emptyList())), KravResponse::class.java
    )

    Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    Assertions.assertThat(response.body?.konteringsfeil?.get(0)?.feilkode).isEqualTo("TOLKNING")
  }

}