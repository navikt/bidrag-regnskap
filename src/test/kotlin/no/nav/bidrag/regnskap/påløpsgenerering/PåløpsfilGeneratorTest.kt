package no.nav.bidrag.regnskap.påløpsgenerering

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.regnskap.dto.Transaksjonskode
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class PåløpsfilGeneratorTest {

  @InjectMockKs
  private lateinit var påløpsfilGenerator: PåløpsfilGenerator

  @Test
  fun `skal skrive påløpsfil`() {

    val oppdrag = TestData.opprettOppdrag(oppdragId = 1)
    val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag)
    val kontering1 = TestData.opprettKontering(
      konteringId = 1,
      transaksjonskode = Transaksjonskode.A1.toString(),
      oppdragsperiode = oppdragsperiode
    )
    val kontering2 = TestData.opprettKontering(
      konteringId = 2,
      transaksjonskode = Transaksjonskode.B1.toString(),
      oppdragsperiode = oppdragsperiode)
    val konteringer = listOf(kontering1, kontering2)
    oppdrag.oppdragsperioder = listOf(oppdragsperiode)
    oppdragsperiode.konteringer = konteringer

    val oppdrag2 = TestData.opprettOppdrag(oppdragId = 2)
    val oppdragsperiode2 = TestData.opprettOppdragsperiode(oppdrag = oppdrag2)
    val kontering3 = TestData.opprettKontering(
      konteringId = 3,
      transaksjonskode = Transaksjonskode.H1.toString(),
      oppdragsperiode = oppdragsperiode2)
    val konteringer2 = listOf(kontering3)
    oppdrag2.oppdragsperioder = listOf(oppdragsperiode2)
    oppdragsperiode2.konteringer = konteringer2


    påløpsfilGenerator.skrivPåløpsfil(konteringer + konteringer2)
  }
}