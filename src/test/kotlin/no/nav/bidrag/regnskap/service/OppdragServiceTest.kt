package no.nav.bidrag.regnskap.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.bidrag.regnskap.utils.TestDataGenerator.genererPersonnummer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class OppdragServiceTest {

  @MockK
  private lateinit var persistenceService: PersistenceService

  @MockK
  private lateinit var overforingTilSkattService: OverforingTilSkattService

  @MockK
  private lateinit var oppdragsperiodeService: OppdragsperiodeService

  @MockK
  private lateinit var konteringService: KonteringService

  @InjectMockKs
  private lateinit var oppdragService: OppdragService

  @Nested
  inner class HentOppdrag {

    @Test
    fun `skal hente eksisterende oppdrag`() {
      val stonadType = StonadType.BIDRAG
      val skyldnerIdent = genererPersonnummer()

      every { persistenceService.hentOppdrag(any()) } returns Optional.of(
        TestData.opprettOppdrag(
          stonadType = stonadType,
          skyldnerIdent = skyldnerIdent
        )
      )
      every { oppdragsperiodeService.hentOppdragsperioderMedKonteringer(any()) } returns emptyList()

      val oppdragResponse = oppdragService.hentOppdrag(1)

      oppdragResponse.stonadType shouldBe stonadType
      oppdragResponse.skyldnerIdent shouldBe skyldnerIdent
    }

    @Test
    fun `skal kaste feil om oppdrag ikke eksisterer`() {
      every { persistenceService.hentOppdrag(any()) } returns Optional.empty()
      shouldThrow<NoSuchElementException> { oppdragService.hentOppdrag(1) }
    }
  }


  @Nested
  inner class LagreOppdrag {

    val oppdragRequest = TestData.opprettOppdragRequest()

    @BeforeEach
    fun setUp() {
      every { persistenceService.lagreOppdrag(any()) } returns 1
    }

    @Test
    fun `skal opprette oppdrag`() {

      every {
        persistenceService.hentOppdragPaUnikeIdentifikatorer(
          any(), any(), any(), any()
        )
      } returns Optional.empty()
      every { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any()) } returns TestData.opprettOppdragsperiode()
      every { konteringService.opprettNyeKonteringer(any(), any()) } returns listOf(TestData.opprettKontering())

      val oppdragId = oppdragService.lagreOppdrag(oppdragRequest)

      oppdragId shouldBe 1
    }

    @Test
    fun `skal ikke opprette oppdrag om det allerede eksisterer`() {
      every {
        persistenceService.hentOppdragPaUnikeIdentifikatorer(
          any(), any(), any(), any()
        )
      } returns Optional.of(TestData.opprettOppdrag())

      val exception = shouldThrow<HttpClientErrorException> { oppdragService.lagreOppdrag(oppdragRequest) }
      exception.message shouldStartWith "400 Kombinasjonen av stonadType, kravhaverIdent, " + "skyldnerIdent og referanse viser til et allerede opprettet oppdrag"
    }
  }


  @Nested
  inner class OppdaterOppdrag {

    @BeforeEach
    fun setUp() {
      every { persistenceService.lagreOppdrag(any()) } returns 1
    }

    @Test
    fun `skal oppdatere oppdrag`() {
      val oppdrag = TestData.opprettOppdrag()

      every { konteringService.finnAlleOverforteKontering(any()) } returns listOf(TestData.opprettKontering())
      every { konteringService.opprettErstattendeKonteringer(any(), any()) } returns listOf(TestData.opprettKontering())
      every { konteringService.opprettNyeKonteringer(any(), any(), any()) } returns listOf(TestData.opprettKontering())
      every { overforingTilSkattService.sendKontering(any(), any()) } returns ResponseEntity.ok().build<Any>()
      every {
        persistenceService.hentOppdragPaUnikeIdentifikatorer(
          any(), any(), any(), any()
        )
      } returns Optional.of(oppdrag)
      every {
        oppdragsperiodeService.setGamleOppdragsperiodeTilInaktivOgOpprettNyOppdragsperiode(
          any(),
          any()
        )
      } returns TestData.opprettOppdragsperiode()

      oppdragService.oppdaterOppdrag(
        TestData.opprettOppdragRequest(
          periodeFra = LocalDate.now().minusMonths(4).withDayOfMonth(1),
          periodeTil = LocalDate.now().plusMonths(4).withDayOfMonth(1)
        )
      )

      verify { persistenceService.lagreOppdrag(oppdrag) }
    }
  }
}