package no.nav.bidrag.regnskap.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class PåløpskjøringServiceTest {

  @MockK(relaxed = true)
  private lateinit var persistenceService: PersistenceService

  @MockK(relaxed = true)
  private lateinit var konteringService: KonteringService

  @InjectMockKs
  private lateinit var påløpskjøringService: PåløpskjøringService

  @Test
  fun `skal ved påløpskjøring kun starte eldste ikke kjørte påløpsperiode`() {
    val påløp1 = TestData.opprettPåløp(påløpId = 1, fullførtTidspunkt = null, forPeriode = "2022-01")
    val påløp2 = TestData.opprettPåløp(påløpId = 2, fullførtTidspunkt = null, forPeriode = "2022-02")
    val påløpListe = listOf(påløp1, påløp2)

    every { persistenceService.hentIkkeKjørtePåløp() } returns påløpListe
    every { persistenceService.hentAlleOppdragsperioderSomErAktiveForPeriode(any()) } returns emptyList()

    val response = påløpskjøringService.startPåløpskjøring()

    response.statusCode shouldBe HttpStatus.CREATED
    verify { persistenceService.lagrePåløp(påløp1) }
    verify(exactly = 0) { persistenceService.lagrePåløp(påløp2) }
    påløp1.fullførtTidspunkt shouldNotBe null
    påløp2.fullførtTidspunkt shouldBe null
  }

  @Test
  fun `skal om det ikke finnes ikke kjørte påløpsperioder returnere 204`() {
    every { persistenceService.hentIkkeKjørtePåløp() } returns emptyList()

    val response = påløpskjøringService.startPåløpskjøring()

    response.statusCode shouldBe HttpStatus.NO_CONTENT
  }

  @Test
  fun `skal ikke opprette konteringer for utgåtte oppdragsperioder`() {
    val påløp = TestData.opprettPåløp(forPeriode = "2022-03")
    val utgåttOppdragsperiode = TestData.opprettOppdragsperiode(
      periodeFra = LocalDate.of(2022, 1, 1),
      periodeTil = LocalDate.of(2022, 3, 1)
    )
    val oppdragsperioder = listOf(utgåttOppdragsperiode)

    every { persistenceService.hentAlleOppdragsperioderSomErAktiveForPeriode(any()) } returns oppdragsperioder

    påløpskjøringService.startPåløpskjøring(påløp)

    verify { persistenceService.lagreOppdragsperiode(utgåttOppdragsperiode) }
    verify { konteringService.opprettLøpendeKonteringerPåOppdragsperioder(emptyList(), påløp.forPeriode) }
  }

  @Test
  fun `skal opprette konteringer for løpende oppdragsperioder`() {
    val påløp = TestData.opprettPåløp(forPeriode = "2022-03")
    val løpendeOppdragsperiode = TestData.opprettOppdragsperiode(
      periodeFra = LocalDate.of(2022, 1, 1),
      periodeTil = LocalDate.of(2022, 4, 1)
    )
    val oppdragsperioder = listOf(løpendeOppdragsperiode)

    every { persistenceService.hentAlleOppdragsperioderSomErAktiveForPeriode(any()) } returns oppdragsperioder

    påløpskjøringService.startPåløpskjøring(påløp)

    verify(exactly = 0) { persistenceService.lagreOppdragsperiode(any()) }
    verify { konteringService.opprettLøpendeKonteringerPåOppdragsperioder(oppdragsperioder, påløp.forPeriode) }
  }
}