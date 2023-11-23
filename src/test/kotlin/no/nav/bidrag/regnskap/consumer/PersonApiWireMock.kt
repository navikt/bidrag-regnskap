package no.nav.bidrag.regnskap.consumer

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

class PersonApiWireMock {

    companion object {
        private const val PORT = 8099
    }

    val nyIdent = "00000000000"

    private val mock = WireMockServer(PORT)

    init {
        mock.start()
    }

    internal fun personidentMedGyldigResponse() {
        mock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/personidenter")).willReturn(
                WireMock.aResponse().withHeader("Content-Type", "application/json").withStatus(200).withBody(
                    """
            [
                {
                    "ident": "$nyIdent",
                    "historisk": false,
                    "gruppe": "FOLKEREGISTERIDENT"
                }
            ]
               """,
                ),
            ),
        )
    }

    internal fun personidentMedNoBody() {
        mock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/personidenter")).willReturn(
                WireMock.aResponse().withHeader("Content-Type", "application/json").withStatus(204),
            ),
        )
    }
}
