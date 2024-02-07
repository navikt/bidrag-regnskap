package no.nav.bidrag.regnskap.service

import io.kotest.matchers.shouldBe
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OppdragsperiodeServiceTest {

    @InjectMockKs
    private lateinit var oppdragsperiodeService: OppdragsperiodeService

    @Nested
    inner class OpprettOppdragsperioder {

        @Test
        fun `Skal opprette nye oppdragsperioder`() {
            val now = LocalDate.now()
            val hendelse = TestData.opprettHendelse(
                periodeListe = listOf(
                    TestData.opprettPeriodeDomene(
                        periodeFomDato = now.minusMonths(5),
                        periodeTilDato = now.minusMonths(3),
                        beløp = BigDecimal.valueOf(7500),
                    ),
                    TestData.opprettPeriodeDomene(
                        periodeFomDato = now.minusMonths(3),
                        periodeTilDato = now,
                        beløp = BigDecimal.valueOf(7600),
                    ),
                ),
            )
            val oppdrag = TestData.opprettOppdrag(mottakerIdent = hendelse.mottakerIdent)

            val nyeOppdragsperioder = hendelse.periodeListe.map { periode ->
                oppdragsperiodeService.opprettNyOppdragsperiode(hendelse, periode, oppdrag)
            }

            nyeOppdragsperioder[0].oppdrag?.mottakerIdent shouldBe hendelse.mottakerIdent
            nyeOppdragsperioder[0].beløp shouldBe hendelse.periodeListe[0].beløp
            nyeOppdragsperioder[0].periodeFra shouldBe hendelse.periodeListe[0].periodeFomDato
            nyeOppdragsperioder[0].periodeTil shouldBe hendelse.periodeListe[0].periodeTilDato
            nyeOppdragsperioder[1].oppdrag?.mottakerIdent shouldBe hendelse.mottakerIdent
            nyeOppdragsperioder[1].beløp shouldBe hendelse.periodeListe[1].beløp
            nyeOppdragsperioder[1].periodeFra shouldBe hendelse.periodeListe[1].periodeFomDato
            nyeOppdragsperioder[1].periodeTil shouldBe hendelse.periodeListe[1].periodeTilDato
        }
    }

    @Nested
    inner class SettAktivTilDato {

        val periodeTil = LocalDate.now().plusMonths(2)
        lateinit var oppdrag: Oppdrag

        @BeforeEach
        fun setup() {
            oppdrag = TestData.opprettOppdrag(
                oppdragsperioder = listOf(
                    TestData.opprettOppdragsperiode(
                        periodeFra = LocalDate.now(),
                        periodeTil = periodeTil,
                        aktivTil = null,
                    ),
                ),
            )
        }

        @Test
        fun `Skal sette aktivTil dato til periodeTil om periodeTil er satt og nye oppdragsperiodens periodeFra er etter periodeTil`() {
            oppdragsperiodeService.settAktivTilDatoPåEksisterendeOppdragsperioder(
                oppdrag,
                LocalDate.now().plusMonths(3),
            )

            oppdrag.oppdragsperioder.first().aktivTil shouldBe periodeTil
        }

        @Test
        fun `Skal sette aktivTil dato til nye oppdragsperiodens periodeFra om periodens periodeFra er før nye oppdragsperiodens periodeFra`() {
            val nyOppdragsperiodePeriodeFra = LocalDate.now().minusMonths(1)
            oppdragsperiodeService.settAktivTilDatoPåEksisterendeOppdragsperioder(oppdrag, nyOppdragsperiodePeriodeFra)

            oppdrag.oppdragsperioder.first().aktivTil shouldBe nyOppdragsperiodePeriodeFra
        }

        @Test
        fun `Skal sette aktivTil dato til nye oppdragsperiodens periodeFra om nye oppdragsperiodens periodeFra er midt i perioden`() {
            val nyOppdragsperiodePeriodeFra = LocalDate.now().plusMonths(1)

            oppdragsperiodeService.settAktivTilDatoPåEksisterendeOppdragsperioder(oppdrag, nyOppdragsperiodePeriodeFra)

            oppdrag.oppdragsperioder.first().aktivTil shouldBe nyOppdragsperiodePeriodeFra
        }

        @Test
        fun `Skal ikke oppdatere aktivTil om aktivTil er satt lenger tilbake i tid enn nye oppdragsperiodens periodeFra`() {
            val now = LocalDate.now()
            oppdrag.oppdragsperioder.first().aktivTil = now

            oppdragsperiodeService.settAktivTilDatoPåEksisterendeOppdragsperioder(
                oppdrag,
                LocalDate.now().plusMonths(1),
            )

            oppdrag.oppdragsperioder.first().aktivTil shouldBe now
        }
    }
}
