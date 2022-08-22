package no.nav.bidrag.regnskap.controller

import StubUtils
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.regnskap.BidragRegnskapLocal
import no.nav.bidrag.regnskap.model.HentPersonResponse
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles


@ActiveProfiles("test")
@SpringBootTest(classes = [BidragRegnskapLocal::class, StubUtils::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
class ExampleControllerTest {

    @LocalServerPort
    private val port = 0

    @Autowired
    lateinit var stubUtils: StubUtils
    @Autowired
    lateinit var httpHeaderTestRestTemplate: HttpHeaderTestRestTemplate

    @Test
    fun `Skal hente persondata`(){
        stubUtils.stubBidragPersonResponse(HentPersonResponse("123213", "Navn Navnesen", "213213213"))
        val response = httpHeaderTestRestTemplate.exchange("${rootUri()}//213213213213",    HttpMethod.GET, null, HentPersonResponse::class.java)

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

    }

    fun rootUri(): String{
        return "http://localhost:$port"
    }

}