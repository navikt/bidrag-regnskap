package no.nav.bidrag.regnskap.service

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.domene.enums.regnskap.Type
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
    inner class OpprettKontering {
        @Test
        fun `Skal opprette nye konteringer`() {
            val oppdragsperiode = TestData.opprettOppdragsperiode(
                konteringer = emptyList(),
                periodeFra = LocalDate.now().minusMonths(3).withDayOfMonth(1),
                periodeTil = LocalDate.now().minusMonths(1).withDayOfMonth(1),
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
                    type = Type.NY.name,
                    overforingstidspunkt = LocalDateTime.now(),
                ),
                TestData.opprettKontering(
                    konteringId = 2,
                    transaksjonskode = transaksjonskode.toString(),
                    overforingsperiode = overforingsperiode.plusMonths(1).toString(),
                    type = Type.ENDRING.name,
                    overforingstidspunkt = LocalDateTime.now(),
                ),
            )
            val nyOppdragsperiode = TestData.opprettOppdragsperiode(periodeFra = now, periodeTil = now.plusMonths(2))
            val oppdrag = TestData.opprettOppdrag(
                oppdragsperioder = listOf(
                    TestData.opprettOppdragsperiode(
                        konteringer = konteringer,
                    ),
                ),
            )
            val hendelse = TestData.opprettHendelse()

            val sisteOverførtePeriode = YearMonth.of(now.year, now.month).plusMonths(5)

            shouldNotThrowAny { konteringService.opprettKorreksjonskonteringer(oppdrag, nyOppdragsperiode, sisteOverførtePeriode, hendelse) }
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
                    type = Type.NY.name,
                ),
            )

            val nyOppdragsperiode = TestData.opprettOppdragsperiode(periodeFra = now, periodeTil = now.plusMonths(2))
            val oppdrag = TestData.opprettOppdrag(
                oppdragsperioder = listOf(
                    TestData.opprettOppdragsperiode(
                        konteringer = konteringer,
                    ),
                ),
            )
            val hendelse = TestData.opprettHendelse()

            val sisteOverførtePeriode = YearMonth.of(now.year, now.month).plusMonths(5)

            konteringService.opprettKorreksjonskonteringer(oppdrag, nyOppdragsperiode, sisteOverførtePeriode, hendelse)

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
                                overforingstidspunkt = overforingstidspunkt,
                            ),
                            TestData.opprettKontering(
                                overforingstidspunkt = overforingstidspunkt.minusMonths(1),
                            ),
                            TestData.opprettKontering(
                                overforingstidspunkt = null,
                            ),
                        ),
                    ),
                ),
            )

            val overforteKonteringerListe = konteringService.hentAlleKonteringerForOppdrag(oppdrag)

            overforteKonteringerListe shouldHaveSize 3
        }
    }
}
