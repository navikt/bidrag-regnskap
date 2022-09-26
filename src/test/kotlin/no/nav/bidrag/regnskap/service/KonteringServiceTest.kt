package no.nav.bidrag.regnskap.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.utils.TestDataGenerator.genererPersonnummer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

@ExtendWith(MockKExtension::class)
class KonteringServiceTest {

  @MockK
  private lateinit var persistenceService: PersistenceService

  @MockK
  private lateinit var skattConsumer: SkattConsumer

  @InjectMockKs
  private lateinit var konteringService: KonteringService

  val oppdragsperiodeId = 1
  val oppdragsId = 1
  val now = LocalDate.now()

  @Test
  @Suppress("NonAsciiCharacters")
  fun `skal sende kontering til skatt når oppdragsperioden er innenfor innsendt periode`() {
    every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragOptionalForPeriode(
      now.minusMonths(3), now.plusMonths(1)
    )

    every { skattConsumer.sendKontering(any()) } returns ResponseEntity.ok(null)

    val restResponse = konteringService.sendKontering(oppdragId = oppdragsId, YearMonth.of(now.year, now.month))

    verify(exactly = 1) { skattConsumer.sendKontering(any()) }

    restResponse.statusCode shouldBe HttpStatus.OK
  }

  @Test
  @Suppress("NonAsciiCharacters")
  fun `skal sende kontering om perioden kun er for en måned`() {

    every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragOptionalForPeriode(
      now, now.plusMonths(1)
    )
    every { skattConsumer.sendKontering(any()) } returns ResponseEntity.ok(null)

    val restResponse = konteringService.sendKontering(oppdragId = oppdragsId, YearMonth.of(now.year, now.month))

    restResponse.statusCode shouldBe HttpStatus.OK
  }

  @Test
  @Suppress("NonAsciiCharacters")
  fun `skal ikke sende kontering til skatt når oppdragsperioden er utenfor innsendt periode`() {
    every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragOptionalForPeriode(
      now.minusMonths(3), now.minusMonths(1)
    )

    val restResponse = konteringService.sendKontering(oppdragId = oppdragsId, YearMonth.of(now.year, now.month))

    restResponse.statusCode shouldBe HttpStatus.NO_CONTENT
  }

  @Test
  fun `skal ikke sende kontering om det finnes flere aktive perioder`() {
    every { persistenceService.hentOppdrag(oppdragsId) } returns Optional.of(
      Oppdrag(
        oppdragId = oppdragsId,
        stonadType = StonadType.BIDRAG.toString(),
        kravhaverIdent = "123456789",
        skyldnerIdent = "987654321",
        sakId = 123456,
        oppdragsperioder = listOf(
          opprettOppdragsperiode(now.minusMonths(3), now.plusMonths(1)),
          opprettOppdragsperiode(now.minusMonths(2), now.plusMonths(1))
        )
      )
    )

    val restResponse = konteringService.sendKontering(oppdragId = oppdragsId, YearMonth.of(now.year, now.month))

    restResponse.statusCode shouldBe HttpStatus.I_AM_A_TEAPOT //TODO: Bedre feilhåndtering
  }

  @Test
  fun `skal ikke sende kontering om oppdrag ikke finens`() {
    every { persistenceService.hentOppdrag(oppdragsId) } returns Optional.empty()

    val restResponse = konteringService.sendKontering(oppdragId = oppdragsId, YearMonth.of(now.year, now.month))

    restResponse.statusCode shouldBe HttpStatus.NOT_FOUND
  }

  private fun opprettOppdragOptionalForPeriode(periodeFra: LocalDate, periodeTil: LocalDate): Optional<Oppdrag> {
    return Optional.of(
      Oppdrag(
        oppdragId = oppdragsId,
        stonadType = StonadType.BIDRAG.toString(),
        kravhaverIdent = "123456789",
        skyldnerIdent = "987654321",
        sakId = 123456,
        oppdragsperioder = listOf(opprettOppdragsperiode(periodeFra, periodeTil))
      )
    )
  }

  private fun opprettOppdragsperiode(periodeFra: LocalDate, periodeTil: LocalDate): Oppdragsperiode {
    return Oppdragsperiode(
      oppdragsperiodeId = oppdragsperiodeId,
      vedtakId = 123,
      gjelderIdent = genererPersonnummer(),
      mottakerIdent = genererPersonnummer(),
      belop = 7500,
      valuta = "NOK",
      periodeFra = periodeFra,
      periodeTil = periodeTil,
      vedtaksdato = now,
      opprettetAv = "MEG",
      delytelseId = "DelytelsesId"
    )
  }
}