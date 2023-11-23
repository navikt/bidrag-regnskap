package no.nav.bidrag.regnskap.maskinporten

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import no.nav.bidrag.commons.security.maskinporten.MaskinportenConfig
import no.nav.bidrag.regnskap.maskinporten.MaskinportenTestUtils.opprettMaskinportenToken

class MaskinportenWireMock {

    companion object {

        private const val PORT = 8096
        private const val TOKEN_PATH = "/token"
        private const val MASKINPORTEN_MOCK_HOST = "http://localhost:$PORT"
        internal const val CONTENT_TYPE = "application/x-www-form-urlencoded"
        internal const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"

        internal fun createMaskinportenConfig() = MaskinportenConfig(
            tokenUrl = MASKINPORTEN_MOCK_HOST + TOKEN_PATH,
            audience = MASKINPORTEN_MOCK_HOST,
            clientId = "17b3e4e8-8203-4463-a947-5c24021b7742",
            privateKey = RSAKeyGenerator(2048).keyID("123").generate().toString(),
            validInSeconds = 120,
            scope = "skatt:testscope.read skatt:testscope.write",
        )
    }

    private val mock = WireMockServer(PORT)

    init {
        mock.start()
    }

    internal fun reset() {
        mock.resetAll()
    }

    internal fun stop() {
        mock.stop()
    }

    internal fun medGyldigResponseForKunEtKall() {
        mock.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(TOKEN_PATH)).withHeader("Content-Type", WireMock.equalTo(CONTENT_TYPE))
                .inScenario("First time").whenScenarioStateIs(Scenario.STARTED)
                .withRequestBody(WireMock.matching("grant_type=$GRANT_TYPE&assertion=.*")).willReturn(
                    WireMock.ok(
                        """{
                      "access_token" : "${opprettMaskinportenToken(120)}",
                      "token_type" : "Bearer",
                      "expires_in" : 599,
                      "scope" : "difitest:test1"
                    }
                """,
                    ),
                ).willSetStateTo("Ended"),
        )
    }

    internal fun kravMedGyldigResponse() {
        mock.stubFor(
            WireMock.post(WireMock.urlEqualTo(TOKEN_PATH)).withHeader("Content-Type", WireMock.equalTo(CONTENT_TYPE))
                .withRequestBody(WireMock.matching("grant_type=$GRANT_TYPE&assertion=.*")).willReturn(
                    WireMock.ok(
                        """{
                  "access_token" : "${opprettMaskinportenToken(120)}",
                  "token_type" : "Bearer",
                  "expires_in" : 599,
                  "scope" : "difitest:test1"
                }
            """,
                    ),
                ),
        )
    }

    internal fun medUgyldigResponse() {
        mock.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(TOKEN_PATH)).withHeader("Content-Type", WireMock.equalTo(CONTENT_TYPE))
                .withRequestBody(WireMock.matching("grant_type=$GRANT_TYPE&assertion=.*")).willReturn(WireMock.ok("""w""")),
        )
    }

    internal fun med500Error() {
        mock.stubFor(
            WireMock.post(WireMock.urlPathEqualTo(TOKEN_PATH)).withHeader("Content-Type", WireMock.equalTo(CONTENT_TYPE))
                .withRequestBody(WireMock.matching("grant_type=$GRANT_TYPE&assertion=.*"))
                .willReturn(WireMock.serverError().withBody("test body")),
        )
    }
}
