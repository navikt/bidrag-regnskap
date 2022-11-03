package no.nav.bidrag.regnskap.service

import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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

    val påløp = påløpskjøringService.hentPåløp()

    påløp shouldBeSameInstanceAs påløp1
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

    påløpskjøringService.startPåløpskjøring(påløp, false)

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

    påløpskjøringService.startPåløpskjøring(påløp, false)

    verify(exactly = 0) { persistenceService.lagreOppdragsperiode(any()) }
    verify { konteringService.opprettLøpendeKonteringerPåOppdragsperioder(oppdragsperioder, påløp.forPeriode) }
  }
}