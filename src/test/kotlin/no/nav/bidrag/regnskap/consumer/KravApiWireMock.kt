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
               """
                )
            )
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
                          "totaltAntall": 1,
                          "mislyketAntall": 0,
                          "fullfoertAntall": 1
                        }
                        """
                )
            )
        )
    }

    internal fun livenessMedGyldigResponse() {
        mock.stubFor(
            WireMock.get(WireMock.urlEqualTo(SkattConsumer.LIVENESS_PATH)).willReturn(
                WireMock.ok()
            )
        )
    }
}
