package no.nav.bidrag.regnskap.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OppdragsperiodeServiceTest {

  @MockK
  private lateinit var persistenceService: PersistenceService

  @MockK
  private lateinit var konteringService: KonteringService

  @InjectMockKs
  private lateinit var oppdragsperiodeService: OppdragsperiodeService


  @Nested
  inner class HentOppdragsperioder {

    @Test
    fun `Skal hente oppdragsperiode`() {
      val oppdrag = TestData.opprettOppdrag()

      every { konteringService.hentKonteringer(any()) } returns emptyList()

      val opprettetOppdragsperiodeListe = oppdragsperiodeService.hentOppdragsperioderMedKonteringer(oppdrag)

      opprettetOppdragsperiodeListe shouldHaveSize oppdrag.oppdragsperioder!!.size
      opprettetOppdragsperiodeListe[0].sakId shouldBe oppdrag.oppdragsperioder!![0].sakId
    }
  }

  @Nested
  inner class OpprettOppdragsperioder {

    @Test
    fun `Skal opprette ny oppdragsperiode`() {
      val oppdragsRequest = TestData.opprettOppdragRequest()
      val oppdrag = TestData.opprettOppdrag(
        oppdragId = 123, oppdragsperioder = null
      )

      val nyOppdragsperiode = oppdragsperiodeService.opprettNyOppdragsperiode(oppdragsRequest, oppdrag)

      nyOppdragsperiode.oppdrag?.oppdragId shouldBe 123
      nyOppdragsperiode.gjelderIdent shouldBe oppdragsRequest.gjelderIdent
      nyOppdragsperiode.mottakerIdent shouldBe oppdragsRequest.mottakerIdent
      nyOppdragsperiode.opprettetAv shouldBe oppdragsRequest.opprettetAv
      nyOppdragsperiode.sakId shouldBe oppdragsRequest.sakId
      nyOppdragsperiode.belop shouldBe oppdragsRequest.belop
      nyOppdragsperiode.valuta shouldBe oppdragsRequest.valuta
    }

    @Test
    fun `Skal opprette randomUUID om delytelseId ikke er angitt`() {
      val oppdragsRequest = TestData.opprettOppdragRequest(delytelseId = null)
      val oppdrag = TestData.opprettOppdrag()

      val nyOppdragsperiode = oppdragsperiodeService.opprettNyOppdragsperiode(oppdragsRequest, oppdrag)

      nyOppdragsperiode.delytelseId shouldNotBe null
    }

  }

  @Nested
  inner class SettGammelOppdragsperiodeTilInaktiv {

    @Test
    fun `Skal sette gamle oppdragsperioder til inaktiv og opprette ny oppdragsperiode`() {

      every { persistenceService.lagreOppdragsperiode(any()) } returns Unit

      val oppdragsperioder = listOf(
        TestData.opprettOppdragsperiode(aktivTil = null), TestData.opprettOppdragsperiode(aktivTil = LocalDate.now())
      )

      val oppdragRequest = TestData.opprettOppdragRequest()

      val nyOppdragsperiode = oppdragsperiodeService.setAktivTilDatoPaOppdragsperiodeOgOpprettNyOppdragsperiode(
        oppdragsperioder,
        oppdragRequest
      )

      oppdragsperioder shouldHaveSize 2
      oppdragsperioder[0].aktivTil shouldNotBe null
      oppdragsperioder[1].aktivTil shouldNotBe null
      nyOppdragsperiode.aktivTil shouldBe null
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `Skal returnere feil om ingen aktive oppdragsperioder finnes på oppdraget`() {
      val oppdragsperioder = listOf(
        TestData.opprettOppdragsperiode(aktivTil = LocalDate.now()),
        TestData.opprettOppdragsperiode(aktivTil = LocalDate.now())
      )

      val oppdragRequest = TestData.opprettOppdragRequest()

      val exception = shouldThrow<IllegalStateException> {
        oppdragsperiodeService.setAktivTilDatoPaOppdragsperiodeOgOpprettNyOppdragsperiode(
          oppdragsperioder,
          oppdragRequest
        )
      }

      exception.message shouldStartWith "Fant ingen aktiv oppdragsperiode på oppdraget."
    }
  }
}
