package no.nav.bidrag.regnskap.service

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.dto.enumer.Type
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class KonteringServiceTest {

    @InjectMockKs
    private lateinit var konteringService: KonteringService

    @MockK(relaxed = true)
    private lateinit var persistenceService: PersistenceService

    @Nested
    inner class HentKonteringer {
        @Test
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
                            ),
                            TestData.opprettKontering(
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
            val oppdragsperiode = TestData.opprettOppdragsperiode(
                konteringer = emptyList(),
                periodeFra = LocalDate.now().minusMonths(3).withDayOfMonth(1),
                periodeTil = LocalDate.now().minusMonths(1).withDayOfMonth(1)
            )
            val hendelse = TestData.opprettHendelse()

            val sisteOverførtePeriode = YearMonth.of(LocalDate.now().year, LocalDate.now().month)

            konteringService.opprettNyeKonteringerPåOppdragsperiode(oppdragsperiode, hendelse, sisteOverførtePeriode)

            oppdragsperiode.konteringer shouldHaveSize 2
        }

        @Test
        fun `Skal opprette korreksjonskonteringer`() {
            val now = LocalDate.now()
            val transaksjonskode = Transaksjonskode.B1
            val overforingsperiode = YearMonth.of(now.year, now.month)
            val konteringer = listOf(
                TestData.opprettKontering(
                    konteringId = 1,
                    transaksjonskode = transaksjonskode.toString(),
                    overforingsperiode = overforingsperiode.toString(),
                    type = Type.NY.toString(),
                    overforingstidspunkt = LocalDateTime.now()
                ),
                TestData.opprettKontering(
                    konteringId = 2,
                    transaksjonskode = transaksjonskode.toString(),
                    overforingsperiode = overforingsperiode.plusMonths(1).toString(),
                    type = Type.ENDRING.toString(),
                    overforingstidspunkt = LocalDateTime.now()
                )
            )
            val nyOppdragsperiode = TestData.opprettOppdragsperiode(periodeFra = now, periodeTil = now.plusMonths(2))
            val oppdrag = TestData.opprettOppdrag(
                oppdragsperioder = listOf(
                    TestData.opprettOppdragsperiode(
                        konteringer = konteringer
                    )
                )
            )

            val sisteOverførtePeriode = YearMonth.of(now.year, now.month).plusMonths(5)

            shouldNotThrowAny { konteringService.opprettKorreksjonskonteringer(oppdrag, nyOppdragsperiode, sisteOverførtePeriode) }
        }

        @Test
        fun `Skal ikke opprette korreksjonskonteringer for allerede korrigerte konteringer`() {
            val now = LocalDate.now()
            val overforingsperiode = YearMonth.of(now.year, now.month)
            val transaksjonskode = Transaksjonskode.B3

            val konteringer = listOf(
                TestData.opprettKontering(
                    konteringId = 1,
                    transaksjonskode = transaksjonskode.toString(),
                    overforingsperiode = overforingsperiode.toString(),
                    type = Type.NY.toString()
                )
            )

            val nyOppdragsperiode = TestData.opprettOppdragsperiode(periodeFra = now, periodeTil = now.plusMonths(2))
            val oppdrag = TestData.opprettOppdrag(
                oppdragsperioder = listOf(
                    TestData.opprettOppdragsperiode(
                        konteringer = konteringer
                    )
                )
            )

            val sisteOverførtePeriode = YearMonth.of(now.year, now.month).plusMonths(5)

            konteringService.opprettKorreksjonskonteringer(oppdrag, nyOppdragsperiode, sisteOverførtePeriode)

            verify(exactly = 0) { persistenceService.lagreKontering(any()) }
        }
    }

    @Nested
    inner class FinnOverforteKonteringer {

        @Test
        fun `Skal finne alle konteringer`() {
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
                            )
                        )
                    )
                )
            )

            val overforteKonteringerListe = konteringService.hentAlleKonteringerForOppdrag(oppdrag)

            overforteKonteringerListe shouldHaveSize 3
        }
    }
}
