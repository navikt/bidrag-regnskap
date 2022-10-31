package no.nav.bidrag.regnskap.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OppdragsperiodeServiceTest {

  @MockK(relaxed = true)
  private lateinit var konteringService: KonteringService

  @InjectMockKs
  private lateinit var oppdragsperiodeService: OppdragsperiodeService


  @Nested
  inner class HentOppdragsperioder {

    @Test
    fun `Skal hente oppdragsperiode`() {
      val oppdrag = TestData.opprettOppdrag()
      val opprettetOppdragsperiodeListe = oppdragsperiodeService.hentOppdragsperioderMedKonteringer(oppdrag)

      opprettetOppdragsperiodeListe shouldHaveSize oppdrag.oppdragsperioder!!.size
      opprettetOppdragsperiodeListe[0].sakId shouldBe oppdrag.oppdragsperioder!![0].sakId
    }
  }

  @Nested
  inner class OpprettOppdragsperioder {

    @Test
    fun `Skal opprette nye oppdragsperioder`() {
      val now = LocalDate.now()
      val hendelse = TestData.opprettHendelse(
        periodeListe = listOf(
          TestData.opprettPeriode(
            periodeFomDato = now.minusMonths(5), periodeTilDato = now.minusMonths(3), belop = BigDecimal.valueOf(7500)
          ), TestData.opprettPeriode(
            periodeFomDato = now.minusMonths(3), periodeTilDato = now, belop = BigDecimal.valueOf(7600)
          )
        )
      )
      val oppdrag = TestData.opprettOppdrag()


      val nyeOppdragsperioder = oppdragsperiodeService.opprettNyeOppdragsperioder(hendelse, oppdrag)

      nyeOppdragsperioder[0].mottakerIdent shouldBe hendelse.mottakerIdent
      nyeOppdragsperioder[0].beløp shouldBe hendelse.periodeListe[0].belop.intValueExact()
      nyeOppdragsperioder[0].periodeFra shouldBe hendelse.periodeListe[0].periodeFomDato
      nyeOppdragsperioder[0].periodeTil shouldBe hendelse.periodeListe[0].periodeTilDato
      nyeOppdragsperioder[1].mottakerIdent shouldBe hendelse.mottakerIdent
      nyeOppdragsperioder[1].beløp shouldBe hendelse.periodeListe[1].belop.intValueExact()
      nyeOppdragsperioder[1].periodeFra shouldBe hendelse.periodeListe[1].periodeFomDato
      nyeOppdragsperioder[1].periodeTil shouldBe hendelse.periodeListe[1].periodeTilDato
    }

    @Test
    fun `Skal opprette randomUUID om delytelseId ikke er angitt`() {
      val hendelse = TestData.opprettHendelse(
        periodeListe = listOf(
          TestData.opprettPeriode(
            referanse = null
          )
        )
      )
      val oppdrag = TestData.opprettOppdrag()


      val nyeOppdragsperioder = oppdragsperiodeService.opprettNyeOppdragsperioder(hendelse, oppdrag)

      nyeOppdragsperioder[0].delytelseId shouldNotBe null
    }
  }
}
