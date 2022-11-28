package no.nav.bidrag.regnskap.fil.avstemning

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.fil.overføring.FiloverføringTilElinKlient
import no.nav.bidrag.regnskap.persistence.bucket.GcpFilBucket
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ExtendWith(MockKExtension::class)
class AvstemningsfilGeneratorTest {

  @MockK(relaxed = true)
  private lateinit var gcpFilBucket: GcpFilBucket
  @MockK(relaxed = true)
  private lateinit var filoverføringTilElinKlient: FiloverføringTilElinKlient

  @InjectMockKs
  private lateinit var avstemningsfilGenerator: AvstemningsfilGenerator

  @Test
  fun `skal skrive avstemningsfil`() {
    val now = LocalDate.now()
    val nowFormattert = now.format(DateTimeFormatter.ofPattern("yyMMdd")).toString()
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

    avstemningsfilGenerator.skrivAvstemningfil(konteringer + konteringer2, now)

    verify(exactly = 1) { gcpFilBucket.finnesFil("avstemning/avstdet_D$nowFormattert.xml") }
    verify(exactly = 1) { gcpFilBucket.lagreFil("avstemning/avstdet_D$nowFormattert.xml", any()) }
    verify(exactly = 1) { gcpFilBucket.lagreFil("avstemning/avstsum_D$nowFormattert.xml", any()) }
    verify(exactly = 1) { filoverføringTilElinKlient.lastOppFilTilFilsluse("avstemning/", "avstsum_D$nowFormattert.xml") }
    verify(exactly = 1) { filoverføringTilElinKlient.lastOppFilTilFilsluse("avstemning/", "avstdet_D$nowFormattert.xml") }
  }

  @Test
  fun `skal skrive tom avstemningsfil`() {
    val now = LocalDate.now()
    val nowFormattert = now.format(DateTimeFormatter.ofPattern("yyMMdd")).toString()

    avstemningsfilGenerator.skrivAvstemningfil(emptyList(), now)

    verify(exactly = 1) { gcpFilBucket.finnesFil("avstemning/avstdet_D$nowFormattert.xml") }
    verify(exactly = 1) { gcpFilBucket.lagreFil("avstemning/avstdet_D$nowFormattert.xml", any()) }
    verify(exactly = 1) { gcpFilBucket.lagreFil("avstemning/avstsum_D$nowFormattert.xml", any()) }
    verify(exactly = 1) { filoverføringTilElinKlient.lastOppFilTilFilsluse("avstemning/", "avstsum_D$nowFormattert.xml") }
    verify(exactly = 1) { filoverføringTilElinKlient.lastOppFilTilFilsluse("avstemning/", "avstdet_D$nowFormattert.xml") }
  }
}

