package no.nav.bidrag.regnskap.consumer

import StubUtils
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.regnskap.BidragRegnskapLocal
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(
  classes = [BidragRegnskapLocal::class, StubUtils::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
class SkattConsumerTest {

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

//  @Test TODO(): Vurdere om wiremock er hensiktsmessig å ta i bruk
//  fun `Skal lagre krav konteringer hos Skatteetaten`() {
//    stubUtils.stubKravResponse(null, HttpStatus.OK)
//
//    val response = httpHeadTestRestTemplate.exchange(
//      "http://localhost:$port/skatt/api/krav",
//      HttpMethod.POST,
//      HttpEntity(SkattKonteringerRequest(emptyList())),
//      SkattKonteringerResponse::class.java
//    )
//
//    Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
//  }
//
//  @Test
//  fun `Skal ta i mot feil i konteringer fra Skatteetaten`() {
//    stubUtils.stubKravResponse(
//      SkattKonteringerResponse(
//        konteringsfeil = listOf(
//          Konteringsfeil(
//            "TOLKNING", "Tolkning feilet i Elin.", KonteringId(
//              Transaksjonskode.B1, YearMonth.parse("2022-01"), "123456789"
//            )
//          )
//        )
//      ), HttpStatus.BAD_REQUEST
//    )
//
//    val response = httpHeadTestRestTemplate.exchange(
//      "http://localhost:$port/skatt/api/krav",
//      HttpMethod.POST,
//      HttpEntity(SkattKonteringerRequest(emptyList())),
//      SkattKonteringerResponse::class.java
//    )
//
//    Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
//    Assertions.assertThat(response.body?.konteringsfeil?.get(0)?.feilkode).isEqualTo("TOLKNING")
//  }
}