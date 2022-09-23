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
  fun `skal hente hente oppdragsperiode og sende kontering til skatt når oppdragsperioden er innenfor innsendt periode`() {
    every { persistenceService.hentOppdragsperiodePaOppdragsIdSomErAktiv(oppdragsperiodeId) } returns listOf(
      opprettOppdragsperiode(now.minusMonths(3), now.plusMonths(1))
    )
    every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragOptional()
    every { skattConsumer.sendKontering(any()) } returns ResponseEntity.ok(null)

    val restResponse = konteringService.sendKontering(oppdragId = oppdragsId, YearMonth.of(now.year, now.month))

    verify(exactly = 1) { skattConsumer.sendKontering(any()) }

    restResponse.statusCode shouldBe HttpStatus.OK
  }

  @Test
  @Suppress("NonAsciiCharacters")
  fun `skal hente oppdragsperiode og sende kontering om perioden kun er for en måned`() {
    every { persistenceService.hentOppdragsperiodePaOppdragsIdSomErAktiv(oppdragsperiodeId) } returns listOf(
      opprettOppdragsperiode(now, now.plusMonths(1))
    )
    every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragOptional()
    every { skattConsumer.sendKontering(any()) } returns ResponseEntity.ok(null)

    val restResponse = konteringService.sendKontering(oppdragId = oppdragsId, YearMonth.of(now.year, now.month))

    restResponse.statusCode shouldBe HttpStatus.OK
  }

  @Test
  @Suppress("NonAsciiCharacters")
  fun `skal hente oppdragsperiode og ikke sende kontering til skatt når oppdragsperioden er utenfor innsendt periode`() {
    every { persistenceService.hentOppdragsperiodePaOppdragsIdSomErAktiv(oppdragsperiodeId) } returns listOf(
      opprettOppdragsperiode(now.minusMonths(3), now.minusMonths(1))
    )

    val restResponse = konteringService.sendKontering(oppdragId = oppdragsId, YearMonth.of(now.year, now.month))

    restResponse.statusCode shouldBe HttpStatus.NO_CONTENT
  }

  @Test
  fun `skal hente oppdragsperiode og ikke om det finnes flere aktive perioder`() {
    every { persistenceService.hentOppdragsperiodePaOppdragsIdSomErAktiv(oppdragsperiodeId) } returns listOf(
      opprettOppdragsperiode(now.minusMonths(3), now.plusMonths(1)),
      opprettOppdragsperiode(now.minusMonths(2), now.plusMonths(1))
    )

    val restResponse = konteringService.sendKontering(oppdragId = oppdragsId, YearMonth.of(now.year, now.month))

    restResponse.statusCode shouldBe HttpStatus.I_AM_A_TEAPOT //TODO: Bedre feilhåndtering
  }

  private fun opprettOppdragOptional() =
    Optional.of(Oppdrag(oppdragsId, StonadType.BIDRAG, "123456789", "987654321", 123456, null, null))

  private fun opprettOppdragsperiode(periodeFra: LocalDate, periodeTil: LocalDate) = Oppdragsperiode(
    oppdragsperiodeId = oppdragsperiodeId,
    oppdragId = oppdragsId,
    vedtakId = 123,
    gjelderIdent = genererPersonnummer(),
    mottakerIdent = genererPersonnummer(),
    belop = 7500,
    valuta = "NOK",
    periodeFra = periodeFra,
    periodeTil = periodeTil,
    vedtaksdato = now,
    opprettetAv = "MEG",
    delytelseId = "DelytelsesId",
    aktiv = true,
    erstatterPeriode = null,
    tekst = null
  )
}