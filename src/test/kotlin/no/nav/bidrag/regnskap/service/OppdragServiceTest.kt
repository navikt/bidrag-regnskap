package no.nav.bidrag.regnskap.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.behandling.felles.enums.EngangsbelopType
import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.regnskap.hendelse.krav.SendKravQueue
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.bidrag.regnskap.utils.TestDataGenerator.genererPersonnummer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class OppdragServiceTest {

  @MockK
  private lateinit var persistenceService: PersistenceService

  @MockK
  private lateinit var oppdragsperiodeService: OppdragsperiodeService

  @MockK
  private lateinit var konteringService: KonteringService

  @MockK
  private lateinit var sendKravQueue: SendKravQueue

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
          stonadType = stonadType, skyldnerIdent = skyldnerIdent
        )
      )
      every { oppdragsperiodeService.hentOppdragsperioderMedKonteringer(any()) } returns emptyList()

      val oppdragResponse = oppdragService.hentOppdrag(1)

      oppdragResponse.type shouldBe stonadType
      oppdragResponse.skyldnerIdent shouldBe skyldnerIdent
    }

    @Test
    fun `skal kaste feil om oppdrag ikke eksisterer`() {
      every { persistenceService.hentOppdrag(any()) } returns Optional.empty()
      shouldThrow<NoSuchElementException> { oppdragService.hentOppdrag(1) }
    }
  }


  @Nested
  inner class OpprettOppdrag {
    @BeforeEach
    fun setUp() {
      every { persistenceService.lagreOppdrag(any()) } returns 1
    }

    @Test
    fun `skal opprette oppdrag`() {
      val hendelse = TestData.opprettHendelse()

      every { oppdragsperiodeService.opprettNyeOppdragsperioder(any(), any()) } returns listOf(TestData.opprettOppdragsperiode())
      every { konteringService.opprettNyeKonteringerPaOppdragsperioder(any(), any(), any()) } returns Unit
      every { sendKravQueue.leggTil(any()) } returns Unit

      val oppdragId = oppdragService.opprettNyttOppdrag(hendelse)

      oppdragId shouldBe 1
    }

    @Test
    @Suppress("NonAscIICharacters")
    fun `skal opprette oppdrag med engangsbeløpId satt`() {
      val hendelse = TestData.opprettHendelse(engangsbelopId = 123, type = EngangsbelopType.GEBYR_MOTTAKER.name)

      every { oppdragsperiodeService.opprettNyeOppdragsperioder(any(), any()) } returns listOf(TestData.opprettOppdragsperiode())
      every { konteringService.opprettNyeKonteringerPaOppdragsperioder(any(), any(), any()) } returns Unit
      every { sendKravQueue.leggTil(any()) } returns Unit

      val oppdragId = oppdragService.opprettNyttOppdrag(hendelse)

      oppdragId shouldBe 1
    }
  }


  @Nested
  inner class OppdaterOppdrag {

    @Test
    fun `skal oppdatere oppdrag`() {
      val hendelse = TestData.opprettHendelse()
      val oppdrag = TestData.opprettOppdrag()

      every { oppdragsperiodeService.opprettNyeOppdragsperioder(any(), any()) } returns listOf(TestData.opprettOppdragsperiode())
      every { konteringService.opprettKorreksjonskonteringerForAlleredeOversendteKonteringer(any(), any()) } returns Unit
      every { konteringService.opprettNyeKonteringerPaOppdragsperioder(any(), any(), any()) } returns Unit
      every { sendKravQueue.leggTil(any()) } returns Unit
      every { persistenceService.lagreOppdrag(oppdrag) } returns 1

      oppdragService.oppdaterOppdrag(hendelse, oppdrag)

      verify { persistenceService.lagreOppdrag(oppdrag) }
    }
  }
}