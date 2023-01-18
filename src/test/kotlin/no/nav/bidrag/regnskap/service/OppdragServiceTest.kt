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
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.bidrag.regnskap.utils.TestDataGenerator.genererPersonnummer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class OppdragServiceTest {

  @MockK(relaxed = true)
  private lateinit var persistenceService: PersistenceService

  @MockK(relaxed = true)
  private lateinit var oppdragsperiodeService: OppdragsperiodeService

  @MockK(relaxed = true)
  private lateinit var konteringService: KonteringService

  @InjectMockKs
  private lateinit var oppdragService: OppdragService

  @Nested
  inner class HentOppdrag {

    @Test
    fun `skal hente eksisterende oppdrag`() {
      val stonadType = StonadType.BIDRAG
      val skyldnerIdent = genererPersonnummer()

      every { persistenceService.hentOppdrag(any()) } returns TestData.opprettOppdrag(
        stonadType = stonadType,
        skyldnerIdent = skyldnerIdent
      )

      val oppdragResponse = oppdragService.hentOppdrag(1)

      oppdragResponse.type shouldBe stonadType.name
      oppdragResponse.skyldnerIdent shouldBe skyldnerIdent
    }

    @Test
    fun `skal kaste feil om oppdrag ikke eksisterer`() {
      every { persistenceService.hentOppdrag(any()) } returns null
      shouldThrow<IllegalStateException> { oppdragService.hentOppdrag(1) }
    }
  }


  @Nested
  inner class OpprettOppdrag {

    @Test
    fun `skal opprette oppdrag`() {
      val hendelse = TestData.opprettHendelse()

      every { oppdragsperiodeService.opprettNyeOppdragsperioder(any(), any()) } returns listOf(TestData.opprettOppdragsperiode())

      val oppdragId = oppdragService.opprettNyttOppdrag(hendelse)

      oppdragId shouldBe 0
    }

    @Test
    fun `skal opprette oppdrag med engangsbeløpId satt`() {
      val hendelse = TestData.opprettHendelse(engangsbelopId = 123, type = EngangsbelopType.GEBYR_MOTTAKER.name)

      every { oppdragsperiodeService.opprettNyeOppdragsperioder(any(), any()) } returns listOf(TestData.opprettOppdragsperiode())

      val oppdragId = oppdragService.opprettNyttOppdrag(hendelse)

      oppdragId shouldBe 0
    }
  }


  @Nested
  inner class OppdaterOppdrag {

    @Test
    fun `skal oppdatere oppdrag`() {
      val hendelse = TestData.opprettHendelse()
      val oppdrag = TestData.opprettOppdrag()

      every { oppdragsperiodeService.opprettNyeOppdragsperioder(any(), any()) } returns listOf(TestData.opprettOppdragsperiode())

      oppdragService.oppdaterOppdrag(hendelse, oppdrag)

      verify { persistenceService.lagreOppdrag(oppdrag) }
    }
  }
}