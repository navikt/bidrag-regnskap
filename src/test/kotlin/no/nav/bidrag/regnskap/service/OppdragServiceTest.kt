package no.nav.bidrag.regnskap.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.regnskap.dto.OppdragRequest
import no.nav.bidrag.regnskap.utils.TestDataGenerator.genererPersonnummer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OppdragServiceTest {

  @MockK
  private lateinit var persistenceService: PersistenceService
  @InjectMockKs
  private lateinit var oppdragService: OppdragService

  @Test
  fun `skal opprette konteringer for alle perioder i et oppdrag`() {
    val oppdragRequest = opprettOppdragRequest()

    every { persistenceService.lagreOppdrag(any()) } returns 1
    every { persistenceService.lagreOppdragsperiode(any()) } returns 1
    every { persistenceService.lagreKontering(any()) } returns Unit

    val oppdragId = oppdragService.lagreOppdrag(oppdragRequest)

    verify(exactly = 12) { persistenceService.lagreKontering(any()) }
    oppdragId shouldBe 1
  }

  private fun opprettOppdragRequest(): OppdragRequest {
    val oppdragRequest = OppdragRequest(
      stonadType = StonadType.BIDRAG,
      kravhaverIdent = genererPersonnummer(),
      skyldnerIdent = genererPersonnummer(),
      saksId = 123456,
      referanse = null,
      vedtakId = 654321,
      gjelderIdent = genererPersonnummer(),
      mottakerIdent = genererPersonnummer(),
      belop = 7500,
      valuta = "NOK",
      periodeFra = LocalDate.now().minusMonths(6).withDayOfMonth(1),
      periodeTil = LocalDate.now().plusMonths(6).withDayOfMonth(1),
      vedtaksdato = LocalDate.now(),
      opprettetAv = "SaksbehandlerId",
      delytelseId = "DelytelsesId",
      utsattTilDato = null,
      tekst = null
    )
    return oppdragRequest
  }
}