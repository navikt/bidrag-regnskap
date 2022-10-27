package no.nav.bidrag.regnskap.hendelse

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import no.nav.bidrag.regnskap.BidragRegnskapLocal
import no.nav.bidrag.regnskap.hendelse.vedtak.VedtakHendelseListener
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [BidragRegnskapLocal::class])
@DisplayName("VedtakHendelseListener (test av forretningslogikk)")
@ActiveProfiles("test")
@EnableMockOAuth2Server
class VedtakHendelseListenerTest {

  @Autowired
  private lateinit var vedtakHendelseListener: VedtakHendelseListener

  @Test
  fun `Skal lese vedtakshendelse uten feil`() {
    assertDoesNotThrow {
      vedtakHendelseListener.lesHendelse(
        """
        {
          "vedtakType":"MANUELT",
          "stonadType":"BIDRAG",
          "sakId":"",
          "skyldnerId":"",
          "kravhaverId":"",
          "mottakerId":"",
          "opprettetAv":"",
          "opprettetTimestamp":"2022-01-11T10:00:00.000001",
          "periodeListe":[]
        }
      """.trimIndent()
      )
    }
  }

  @Test
  fun `Skal lese vedtakshendelse med feil`() {
    assertThrows<InvalidFormatException> {
      vedtakHendelseListener.lesHendelse(
        """
        {
          "vedtakType":"ÅRSAVGIFT",
          "stonadType":"BIDRAG",
          "sakId":"",
          "skyldnerId":"",
          "kravhaverId":"",
          "mottakerId":"",
          "opprettetAv":"",
          "opprettetTimestamp":"2022-01-11T10:00:00.000001",
          "periodeListe":[]
        }
      """.trimIndent()
      )
    }
  }
}