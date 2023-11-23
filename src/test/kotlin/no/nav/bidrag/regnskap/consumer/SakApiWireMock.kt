package no.nav.bidrag.regnskap.consumer

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.bidrag.commons.util.PersonidentGenerator
import wiremock.com.google.common.net.HttpHeaders

class SakApiWireMock {

    companion object {
        private const val PORT = 8098
    }

    private val mock = WireMockServer(PORT)

    init {
        mock.start()
    }

    internal fun sakMedGyldigResponse() {
        mock.stubFor(
            WireMock.get(WireMock.anyUrl()).willReturn(
                WireMock.aResponse().withHeader(HttpHeaders.CONTENT_TYPE, "application/json").withStatus(200).withBody(
                    """
           {
             "eierfogd": "eierfogd",
             "saksnummer": "123",
             "saksstatus": "AK",
             "kategori": "N",
             "opprettetDato": "2023-01-01",
             "levdeAdskilt": "false",
             "ukjentPart": "false",
             "roller": [
               {
                 "fødselsnummer": "${PersonidentGenerator.genererFødselsnummer()}",
                 "type": "BA"
               }
             ]
           }
        """,
                ),
            ),
        )
    }
}
