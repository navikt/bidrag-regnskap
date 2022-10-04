package no.nav.bidrag.regnskap.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.regnskap.dto.Transaksjonskode
import no.nav.bidrag.regnskap.dto.Type
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class KonteringServiceTest {

  @InjectMockKs
  private lateinit var konteringService: KonteringService


  @Nested
  inner class HentKonteringer {
    @Test
    @Suppress("NonAsciiCharacters")
    fun `Skal hente alle kontering på et oppdrag`() {
      val transaksjonskode = Transaksjonskode.B1
      val overforingsperiode = YearMonth.now()

      val oppdrag = TestData.opprettOppdrag(
        oppdragsperioder = listOf(
          TestData.opprettOppdragsperiode(
            konteringer = listOf(
              TestData.opprettKontering(
                konteringId = 1,
                transaksjonskode = transaksjonskode.toString(),
                overforingsperiode = overforingsperiode.toString(),
                type = Type.NY.toString()
              ), TestData.opprettKontering(
                konteringId = 2,
                transaksjonskode = transaksjonskode.toString(),
                overforingsperiode = overforingsperiode.plusMonths(1).toString(),
                type = Type.ENDRING.toString()
              )
            )
          )
        )
      )

      val konteringResponseListe = konteringService.hentKonteringer(oppdrag)

      konteringResponseListe[0].konteringId shouldBe 1
      konteringResponseListe[1].konteringId shouldBe 2
      konteringResponseListe[0].transaksjonskode shouldBe transaksjonskode
      konteringResponseListe[1].transaksjonskode shouldBe transaksjonskode
      konteringResponseListe[0].overforingsperiode shouldBe overforingsperiode.toString()
      konteringResponseListe[1].overforingsperiode shouldBe overforingsperiode.plusMonths(1).toString()
      konteringResponseListe[0].type shouldBe Type.NY
      konteringResponseListe[1].type shouldBe Type.ENDRING
    }
  }

  @Nested
  inner class OpprettKontering {
    @Test
    fun `Skal opprette nye konteringer`() {
      val perioderForOppdrag = listOf<YearMonth>(YearMonth.now().minusMonths(1), YearMonth.now())
      val oppdragsperiode = TestData.opprettOppdragsperiode()

      val nyeKonteringer = konteringService.opprettNyeKonteringer(perioderForOppdrag, oppdragsperiode)

      nyeKonteringer[0].overforingsperiode shouldBe YearMonth.now().minusMonths(1).toString()
      nyeKonteringer[1].overforingsperiode shouldBe YearMonth.now().toString()
      nyeKonteringer[0].type shouldBe Type.NY.toString()
      nyeKonteringer[1].type shouldBe Type.ENDRING.toString()
    }

    @Test
    fun `Skal opprette erstattende konteringer`() {
      val transaksjonskode = Transaksjonskode.B1
      val overforingsperiode = YearMonth.now()

      val konteringer = listOf(
        TestData.opprettKontering(
          konteringId = 1,
          transaksjonskode = transaksjonskode.toString(),
          overforingsperiode = overforingsperiode.toString(),
          type = Type.NY.toString()
        ), TestData.opprettKontering(
          konteringId = 2,
          transaksjonskode = transaksjonskode.toString(),
          overforingsperiode = overforingsperiode.plusMonths(1).toString(),
          type = Type.ENDRING.toString()
        )
      )

      val erstattendeKontering = konteringService.opprettErstattendeKonteringer(konteringer, listOf(overforingsperiode))

      erstattendeKontering shouldHaveSize 1
      erstattendeKontering[0].overforingsperiode shouldBe overforingsperiode.toString()
      erstattendeKontering[0].transaksjonskode shouldBe transaksjonskode.korreksjonskode
      erstattendeKontering[0].type shouldBe Type.ENDRING.toString()
    }

    @Test
    fun `Skal ikke opprette erstattende konteringer for korreksjoner`() {
      val transaksjonskode = Transaksjonskode.B3
      val overforingsperiode = YearMonth.now()

      val konteringer = listOf(
        TestData.opprettKontering(
          konteringId = 1,
          transaksjonskode = transaksjonskode.toString(),
          overforingsperiode = overforingsperiode.toString(),
          type = Type.NY.toString()
        )
      )

      val erstattendeKontering = konteringService.opprettErstattendeKonteringer(konteringer, listOf(overforingsperiode))

      erstattendeKontering shouldHaveSize 0
    }
  }

  @Nested
  inner class FinnOverforteKonteringer {

    @Test
    fun `Skal finne alle overforte konteringer`() {

      val overforingstidspunkt = LocalDateTime.now()
      val oppdrag = TestData.opprettOppdrag(
        oppdragsperioder = listOf(
          TestData.opprettOppdragsperiode(
            konteringer = listOf(
              TestData.opprettKontering(
                overforingstidspunkt = overforingstidspunkt
              ),
              TestData.opprettKontering(
                overforingstidspunkt = overforingstidspunkt.minusMonths(1)
              ),
              TestData.opprettKontering(
                overforingstidspunkt = null
              ),
            )
          )
        )
      )

      val overforteKonteringerListe = konteringService.finnAlleOverforteKontering(oppdrag)

      overforteKonteringerListe shouldHaveSize 2
      overforteKonteringerListe[0].overforingstidspunkt shouldBe overforingstidspunkt
      overforteKonteringerListe[1].overforingstidspunkt shouldBe overforingstidspunkt.minusMonths(1)
    }
  }
}