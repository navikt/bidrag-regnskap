package no.nav.bidrag.regnskap.service

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.behandling.felles.enums.EngangsbelopType
import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class VedtakshendelseServiceTest {

  @MockK(relaxed = true)
  private lateinit var oppdragService: OppdragService

  @MockK(relaxed = true)
  private lateinit var kravService: KravService

  @InjectMockKs
  private lateinit var vedtakshendelseService: VedtakshendelseService

  @Test
  fun `skal mappe vedtakshendelse uten feil`() {
    val hendelse = opprettVedtakshendelse()

    val vedtakHendelse = vedtakshendelseService.mapVedtakHendelse(hendelse)

    vedtakHendelse shouldNotBe null
    vedtakHendelse.id shouldBe 123
    vedtakHendelse.engangsbelopListe?.shouldHaveSize(1)
    vedtakHendelse.stonadsendringListe?.shouldHaveSize(1)
  }

  @Test
  fun `skal opprette oppdrag for stonadsendringer og engangsbeløp`() {
    val hendelseCaptor = mutableListOf<Hendelse>()
    val hendelse = opprettVedtakshendelse()

    every { oppdragService.lagreHendelse(capture(hendelseCaptor)) } returns 1

    vedtakshendelseService.behandleHendelse(hendelse)

    verify(exactly = 2) { oppdragService.lagreHendelse(any()) }
    hendelseCaptor[0].type shouldBe StonadType.BIDRAG.name
    hendelseCaptor[1].type shouldBe EngangsbelopType.GEBYR_SKYLDNER.name
  }

  @Test
  fun `Skal lese vedtakshendelse uten feil`() {
    assertDoesNotThrow {
      vedtakshendelseService.mapVedtakHendelse(
        """
        {
          "kilde":"MANUELT",
          "type":"INDEKSREGULERING",
          "id":"779",
          "vedtakTidspunkt":"2022-06-03T00:00:00.000000000",
          "enhetId":"4812",
          "opprettetAv":"B101173",
          "opprettetTidspunkt":"2022-10-19T16:00:23.254988482",
          "stonadsendringListe":[
          ],
          "engangsbelopListe":[
          ],
          "sporingsdata": {
            "correlationId": "12345"
          }
        }
      """.trimIndent()
      )
    }
  }

  @Test
  fun `Skal lese vedtakshendelse med feil`() {
    assertThrows<InvalidFormatException> {
      vedtakshendelseService.mapVedtakHendelse(
        """
        {
          "type":"ÅRSAVGIFT",
          "vedtakTidspunkt":"2022-01-01T00:00:00.000000000",
          "id":"123",
          "enhetId":"enhetid",
          "stonadType":"BIDRAG",
          "sakId":"",
          "skyldnerId":"",
          "kravhaverId":"",
          "mottakerId":"",
          "opprettetAv":"",
          "opprettetTidspunkt":"2022-01-11T10:00:00.000001",
          "periodeListe":[],
          "sporingsdata: {
            "correlationId": "12345"
          }
        }
      """.trimIndent()
      )
    }
  }

  private fun opprettVedtakshendelse(): String {
    return """
      {
        "kilde":"MANUELT",
        "type":"INNKREVING",
        "id":"123",
        "vedtakTidspunkt":"2022-06-01T00:00:00.000000000",
        "enhetId":"4812",
        "opprettetAv":"B111111",
        "opprettetTidspunkt":"2022-01-01T16:00:00.000000000",
        "stonadsendringListe":[
          {
            "type":"BIDRAG",
            "sakId":"456",
            "skyldnerId":"11111111111",
            "kravhaverId":"22222222222",
            "mottakerId":"333333333333",
            "innkreving":"JA",
            "endring":true,
            "periodeListe":[
              {
                "fomDato":"2022-01-01",
                "tilDato":"2022-03-01",
                "belop":"2910",
                "valutakode":"NOK",
                "resultatkode":"KBB"
              },
              {
                "fomDato":"2022-03-01",
                "tilDato":null,
                "belop":"2930",
                "valutakode":"NOK",
                "resultatkode":"KBB"
              }
            ]
          }
        ]
        ,
        "engangsbelopListe":[
          {
            "id":"1",
            "type":"GEBYR_SKYLDNER",
            "sakId":"789",
            "skyldnerId":"11111111111",
            "kravhaverId":"22222222222",
            "mottakerId":"333333333333",
            "belop":"1790",
            "valutakode":"NOK",
            "resultatkode":"GIGI",
            "innkreving":"JA",
            "endring":true
          }
        ],
         "sporingsdata": {
            "correlationId": "12345"
          }
      }"""
  }
}