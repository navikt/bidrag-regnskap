package no.nav.bidrag.regnskap.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.behandling.felles.enums.EngangsbelopType
import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.regnskap.utils.TestData
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
      val skyldnerIdent = PersonidentGenerator.genererPersonnummer()

      every { persistenceService.hentOppdrag(any()) } returns TestData.opprettOppdrag(
        stonadType = stonadType,
        skyldnerIdent = skyldnerIdent
      )

      val oppdragResponse = oppdragService.hentOppdrag(1)!!

      oppdragResponse.type shouldBe stonadType.name
      oppdragResponse.skyldnerIdent shouldBe skyldnerIdent
    }

    @Test
    fun `skal være tom om oppdrag ikke eksisterer`() {
      every { persistenceService.hentOppdrag(any()) } returns null
      oppdragService.hentOppdrag(1) shouldBe null
    }
  }


  @Nested
  inner class OpprettOppdrag {

    @Test
    fun `skal opprette oppdrag`() {
      val hendelse = TestData.opprettHendelse()

      every { oppdragsperiodeService.opprettNyeOppdragsperioder(any(), any()) } returns listOf(TestData.opprettOppdragsperiode())

      val oppdragId = oppdragService.lagreEllerOppdaterOppdrag(null, hendelse)

      oppdragId shouldBe 0
    }

    @Test
    fun `skal opprette oppdrag med engangsbeløpId satt`() {
      val hendelse = TestData.opprettHendelse(engangsbelopId = 123, type = EngangsbelopType.GEBYR_MOTTAKER.name)

      every { oppdragsperiodeService.opprettNyeOppdragsperioder(any(), any()) } returns listOf(TestData.opprettOppdragsperiode())

      val oppdragId = oppdragService.lagreEllerOppdaterOppdrag(null, hendelse)

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

      oppdragService.lagreEllerOppdaterOppdrag(oppdrag, hendelse)

      verify { persistenceService.lagreOppdrag(oppdrag) }
    }
  }
}