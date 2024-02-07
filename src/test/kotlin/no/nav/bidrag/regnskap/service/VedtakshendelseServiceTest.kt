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
import no.nav.bidrag.regnskap.util.IdentUtils
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class VedtakshendelseServiceTest {

    @MockK(relaxed = true)
    private lateinit var oppdragService: OppdragService

    @MockK(relaxed = true)
    private lateinit var kravService: KravService

    @MockK(relaxed = true)
    private lateinit var persistenceService: PersistenceService

    @MockK(relaxed = true)
    private lateinit var identUtils: IdentUtils

    @InjectMockKs
    private lateinit var vedtakshendelseService: VedtakshendelseService

    @BeforeEach
    fun setup() {
        every { persistenceService.harAktivtDriftsavvik() } returns false
        every { kravService.erVedlikeholdsmodusPåslått() } returns false
    }

    @Test
    fun `skal mappe vedtakshendelse uten feil`() {
        val hendelse = opprettVedtakshendelse()

        val vedtakHendelse = vedtakshendelseService.mapVedtakHendelse(hendelse)

        vedtakHendelse shouldNotBe null
        vedtakHendelse.id shouldBe 123
        vedtakHendelse.engangsbeløpListe?.shouldHaveSize(1)
        vedtakHendelse.stønadsendringListe?.shouldHaveSize(1)
    }

    @Test
    fun `skal opprette oppdrag for stonadsendringer og engangsbeløp`() {
        val hendelse = opprettVedtakshendelse()

        every { oppdragService.lagreHendelse(any()) } returns 1

        vedtakshendelseService.behandleHendelse(hendelse)

        verify(exactly = 1) { oppdragService.lagreHendelse(any(), false) }
        verify(exactly = 1) { oppdragService.lagreHendelse(any(), true) }
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
          "vedtakstidspunkt":"2022-06-03T00:00:00.000000000",
          "enhetsnummer":"4812",
          "opprettetAv":"B101173",
          "kildeapplikasjon": "TEST",
          "opprettetTidspunkt":"2022-10-19T16:00:23.254988482",
          "stønadsendringListe":[
          ],
          "engangsbeløpListe":[
          ],
          "sporingsdata": {
            "correlationId": "12345"
          }
        }
                """.trimIndent(),
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
                """.trimIndent(),
            )
        }
    }

    private fun opprettVedtakshendelse(): String {
        return """
      {
        "kilde":"MANUELT",
        "type":"INNKREVING",
        "id":"123",
        "vedtakstidspunkt":"2022-06-01T00:00:00.000000000",
        "enhetsnummer":"4812",
        "opprettetAv":"B111111",
        "kildeapplikasjon":"TEST",
        "opprettetTidspunkt":"2022-01-01T16:00:00.000000000",
        "stønadsendringListe":[
          {
            "type":"BIDRAG",
            "sak":"456",
            "skyldner":"11111111111",
            "kravhaver":"22222222222",
            "mottaker":"333333333333",
            "innkreving":"MED_INNKREVING",
            "beslutning":"ENDRING",
            "periodeListe":[
              {
                "periode": {
                    "fom":"2022-01",
                    "til":"2022-03"
                },
                "beløp":"2910",
                "valutakode":"NOK",
                "resultatkode":"KBB"
              },
              {
                "periode": {
                    "fom":"2022-03",
                    "til":null
                },
                "beløp":"2930",
                "valutakode":"NOK",
                "resultatkode":"KBB"
              }
            ]
          }
        ]
        ,
        "engangsbeløpListe":[
          {
            "type":"GEBYR_SKYLDNER",
            "sak":"789",
            "skyldner":"11111111111",
            "kravhaver":"22222222222",
            "mottaker":"333333333333",
            "belop":"1790",
            "valutakode":"NOK",
            "resultatkode":"GIGI",
            "innkreving":"MED_INNKREVING",
            "referanse":"REFERANSE",
            "beslutning":"ENDRING"
          }
        ],
         "sporingsdata": {
            "correlationId": "12345"
          }
      }"""
    }
}
