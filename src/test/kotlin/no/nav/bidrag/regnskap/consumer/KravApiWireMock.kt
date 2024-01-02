package no.nav.bidrag.regnskap.consumer

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

class KravApiWireMock {

    companion object {
        private const val PORT = 8097
    }

    private val mock = WireMockServer(PORT)

    init {
        mock.start()
    }

    internal fun kravMedGyldigResponse() {
        mock.stubFor(
            WireMock.post(WireMock.urlEqualTo(SkattConsumer.KRAV_PATH)).willReturn(
                WireMock.aResponse().withStatus(202).withBody(
                    """
            {
              "BatchUid": "STUBBED-BATCHUID"
            }
               """,
                ),
            ),
        )
    }

    internal fun behandlingsstatusMedGyldigResponse() {
        mock.stubFor(
            WireMock.get(WireMock.urlEqualTo("${SkattConsumer.KRAV_PATH}/STUBBED-BATCHUID")).willReturn(
                WireMock.aResponse().withHeader("Content-Type", "application/json").withStatus(200).withBody(
                    """
                        {
                          "konteringFeil": [],
                          "batchStatus": "Done",
                          "batchUid": "3f1248e9-8d19-4dc3-9584-84055421753d",
                          "totaltAntallKrav": 1,
                          "mislyketAntallKrav": 0,
                          "fullfoertAntallKrav": 1
                        }
                        """,
                ),
            ),
        )
    }

    internal fun livenessMedGyldigResponse() {
        mock.stubFor(
            WireMock.get(WireMock.urlEqualTo(SkattConsumer.LIVENESS_PATH)).willReturn(
                WireMock.ok(),
            ),
        )
    }
}
