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
class VedtakHendelseServiceTest {

  @MockK(relaxed = true)
  private lateinit var oppdragService: OppdragService

  @MockK(relaxed = true)
  private lateinit var kravService: KravService

  @InjectMockKs
  private lateinit var vedtakHendelseService: VedtakHendelseService

  @Test
  fun `skal mappe vedtakshendelse uten feil`() {
    val hendelse = opprettVedtakshendelse()

    val vedtakHendelse = vedtakHendelseService.mapVedtakHendelse(hendelse)

    vedtakHendelse shouldNotBe null
    vedtakHendelse.vedtakId shouldBe 123
    vedtakHendelse.engangsbelopListe?.shouldHaveSize(1)
    vedtakHendelse.stonadsendringListe?.shouldHaveSize(1)
  }

  @Test
  fun `skal opprette oppdrag for stonadsendringer og engangsbeløp`() {
    val hendelseCaptor = mutableListOf<Hendelse>()
    val hendelse = opprettVedtakshendelse()

    every { oppdragService.lagreHendelse(capture(hendelseCaptor)) } returns 1

    vedtakHendelseService.behandleHendelse(hendelse)

    verify(exactly = 2) { oppdragService.lagreHendelse(any()) }
    hendelseCaptor[0].type shouldBe StonadType.BIDRAG.name
    hendelseCaptor[1].type shouldBe EngangsbelopType.GEBYR_SKYLDNER.name
  }

  @Test
  fun `Skal lese vedtakshendelse uten feil`() {
    assertDoesNotThrow {
      vedtakHendelseService.mapVedtakHendelse(
        """
        {
          "vedtakType":"AUTOMATISK_INDEKSREGULERING",
          "vedtakId":"779",
          "vedtakDato":"2022-06-03",
          "enhetId":"4812",
          "opprettetAv":"B101173",
          "opprettetTidspunkt":"2022-10-19T16:00:23.254988482",
          "stonadsendringListe":[
          ],
          "engangsbelopListe":[
          ]
        }
      """.trimIndent()
      )
    }
  }

  @Test
  fun `Skal lese vedtakshendelse med feil`() {
    assertThrows<InvalidFormatException> {
      vedtakHendelseService.mapVedtakHendelse(
        """
        {
          "vedtakType":"ÅRSAVGIFT",
          "vedtakDato":"2022-01-01",
          "vedtakId":"123",
          "enhetId":"enhetid",
          "stonadType":"BIDRAG",
          "sakId":"",
          "skyldnerId":"",
          "kravhaverId":"",
          "mottakerId":"",
          "opprettetAv":"",
          "opprettetTidspunkt":"2022-01-11T10:00:00.000001",
          "periodeListe":[]
        }
      """.trimIndent()
      )
    }
  }

  private fun opprettVedtakshendelse(): String {
    return "{" +
        "\"vedtakType\":\"MANUELT\",\n" +
        "  \"vedtakId\":\"123\",\n" +
        "  \"vedtakDato\":\"2022-06-01\",\n" +
        "  \"enhetId\":\"4812\",\n" +
        "  \"opprettetAv\":\"B111111\",\n" +
        "  \"opprettetTidspunkt\":\"2022-01-01T16:00:00.000000000\",\n" +
        "  \"stonadsendringListe\":[\n" +
        "    {\n" +
        "      \"stonadType\":\"BIDRAG\",\n" +
        "      \"sakId\":\"456\",\n" +
        "      \"skyldnerId\":\"11111111111\",\n" +
        "      \"kravhaverId\":\"22222222222\",\n" +
        "      \"mottakerId\":\"333333333333\",\n" +
        "      \"periodeListe\":[\n" +
        "        {\n" +
        "          \"periodeFomDato\":\"2022-01-01\",\n" +
        "          \"periodeTilDato\":\"2022-03-01\",\n" +
        "          \"belop\":\"2910\",\n" +
        "          \"valutakode\":\"NOK\",\n" +
        "          \"resultatkode\":\"KBB\"\n" +
        "        },\n" +
        "        {\"periodeFomDato\":\"2022-03-01\",\n" +
        "          \"periodeTilDato\":null,\n" +
        "          \"belop\":\"2930\",\n" +
        "          \"valutakode\":\"NOK\",\n" +
        "          \"resultatkode\":\"KBB\"\n" +
        "        }\n" +
        "      ]\n" +
        "    }\n" +
        "  ]\n" +
        "  ,\n" +
        "  \"engangsbelopListe\":[\n" +
        "    {\n" +
        "      \"engangsbelopId\":\"1\",\n" +
        "      \"type\":\"GEBYR_SKYLDNER\",\n" +
        "      \"sakId\":\"789\",\n" +
        "      \"skyldnerId\":\"11111111111\",\n" +
        "      \"kravhaverId\":\"22222222222\",\n" +
        "      \"mottakerId\":\"333333333333\",\n" +
        "      \"belop\":\"1790\",\n" +
        "      \"valutakode\":\"NOK\",\n" +
        "      \"resultatkode\":\"GIGI\"\n" +
        "    }\n" +
        "  ]\n" +
        "}"
  }
}