package no.nav.bidrag.regnskap.service

import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.bidrag.domene.enums.Vedtakstype
import no.nav.bidrag.domene.enums.regnskap.Søknadstype
import no.nav.bidrag.domene.enums.regnskap.Type
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.fil.overføring.FiloverføringTilElinKlient
import no.nav.bidrag.regnskap.persistence.bucket.GcpFilBucket
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import no.nav.bidrag.regnskap.util.PeriodeUtils.hentAllePerioderMellomDato
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.YearMonth
import java.util.Optional

@ExtendWith(MockKExtension::class)
class PåløpskjøringServiceTest {

    private val oppdragsperiodeRepo = mockk<OppdragsperiodeRepository>(relaxed = true)
    private val persistenceService = mockk<PersistenceService>(relaxed = true)
    private val gcpFilBucket = mockk<GcpFilBucket>(relaxed = true)
    private val filoverføringTilElinKlient = mockk<FiloverføringTilElinKlient>(relaxed = true)
    private val skattConsumer = mockk<SkattConsumer>(relaxed = true)
    private val meterRegistry = mockk<MeterRegistry>(relaxed = true)

    private val påløpskjøringService =
        PåløpskjøringService(
            oppdragsperiodeRepo,
            persistenceService,
            ManglendeKonteringerService(oppdragsperiodeRepo, persistenceService, "2006-04"),
            gcpFilBucket,
            filoverføringTilElinKlient,
            skattConsumer,
            meterRegistry,
        )

    @Test
    fun `Skal ved påløpskjøring kun starte eldste ikke kjørte påløpsperiode`() {
        val påløp1 = TestData.opprettPåløp(påløpId = 1, fullførtTidspunkt = null, forPeriode = "2022-01")
        val påløp2 = TestData.opprettPåløp(påløpId = 2, fullførtTidspunkt = null, forPeriode = "2022-02")
        val påløpListe = listOf(påløp1, påløp2)

        every { persistenceService.hentIkkeKjørtePåløp() } returns påløpListe

        val påløp = påløpskjøringService.hentPåløp()

        påløp shouldBeSameInstanceAs påløp1
    }

    @Test
    fun `Skal opprette konteringer for alle oppdragsperioder som ikke allerede har fått opprettet alle konteringer for en fastsettelse av et bidrag uten korreksjoner`() =
        runTest {
            val påløp = TestData.opprettPåløp(påløpId = 1, forPeriode = "2023-01")

            val oppdrag = TestData.opprettOppdrag(oppdragsperioder = emptyList())
            val oppdragsperiodeMedManglendeKonteringer = TestData.opprettOppdragsperiode(
                periodeFra = LocalDate.of(2022, 1, 1),
                periodeTil = null,
                konteringer = emptyList(),
                aktivTil = null,
                konteringerFullførtOpprettet = false,
                oppdrag = oppdrag,
            )
            oppdrag.oppdragsperioder = listOf(oppdragsperiodeMedManglendeKonteringer)

            val oppdragsperiodeIder = listOf(
                oppdragsperiodeMedManglendeKonteringer.oppdragsperiodeId,
            )
            every { oppdragsperiodeRepo.hentAlleOppdragsperioderSomIkkeHarOpprettetAlleKonteringer() } returns oppdragsperiodeIder
            every {
                oppdragsperiodeRepo.findById(oppdragsperiodeMedManglendeKonteringer.oppdragsperiodeId)
            } returns Optional.of(oppdragsperiodeMedManglendeKonteringer)

            påløpskjøringService.startPåløpskjøring(påløp, false, true)

            val perioderMellomDato = hentAllePerioderMellomDato(
                oppdragsperiodeMedManglendeKonteringer.periodeFra,
                oppdragsperiodeMedManglendeKonteringer.periodeTil,
                YearMonth.parse(påløp.forPeriode),
            )

            val konteringer = oppdragsperiodeMedManglendeKonteringer.konteringer
            konteringer.sortedBy { it.overføringsperiode }

            konteringer shouldHaveSize perioderMellomDato.size
            konteringer.shouldBeUnique()
            konteringer.all { it.type == Type.NY.name } shouldBe true
            konteringer.all { it.søknadType == Søknadstype.EN.name } shouldBe true
            konteringer.forEach { it.sendtIPåløpsperiode shouldBe "2023-01" }
            konteringer.forEachIndexed { index, kontering ->
                val periodeForKontering = perioderMellomDato[index]
                kontering.overføringsperiode shouldBe periodeForKontering.toString()
            }
        }

    @Test
    fun `Skal opprette konteringer for alle oppdragsperioder som ikke allerede har fått opprettet alle konteringer for en fastsettelse av et bidrag med korreksjoner`() =
        runTest {
            val påløp = TestData.opprettPåløp(påløpId = 1, forPeriode = "2023-01")

            val oppdrag = TestData.opprettOppdrag(oppdragsperioder = emptyList())
            val oppdragsperiodeMedManglendeKonteringer1 = TestData.opprettOppdragsperiode(
                oppdragsperiodeId = 1,
                periodeFra = LocalDate.of(2021, 6, 1),
                periodeTil = LocalDate.of(2022, 1, 1),
                konteringer = emptyList(),
                aktivTil = LocalDate.of(2022, 1, 1),
                konteringerFullførtOpprettet = false,
                oppdrag = oppdrag,
            )
            val oppdragsperiodeMedManglendeKonteringer2 = TestData.opprettOppdragsperiode(
                oppdragsperiodeId = 2,
                periodeFra = LocalDate.of(2022, 1, 1),
                periodeTil = null,
                konteringer = emptyList(),
                aktivTil = null,
                konteringerFullførtOpprettet = false,
                oppdrag = oppdrag,
            )
            val oppdragsperioder = listOf(oppdragsperiodeMedManglendeKonteringer1, oppdragsperiodeMedManglendeKonteringer2)
            oppdrag.oppdragsperioder = oppdragsperioder

            val oppdragsperiodeIder = listOf(
                oppdragsperiodeMedManglendeKonteringer1.oppdragsperiodeId,
                oppdragsperiodeMedManglendeKonteringer2.oppdragsperiodeId,
            )
            every { oppdragsperiodeRepo.hentAlleOppdragsperioderSomIkkeHarOpprettetAlleKonteringer() } returns oppdragsperiodeIder
            oppdragsperioder.forEach {
                every { oppdragsperiodeRepo.findById(it.oppdragsperiodeId) } returns Optional.of(it)
            }

            påløpskjøringService.startPåløpskjøring(påløp, false, true)

            val perioderMellomDato = hentAllePerioderMellomDato(
                oppdragsperiodeMedManglendeKonteringer1.periodeFra,
                oppdragsperiodeMedManglendeKonteringer2.periodeTil,
                YearMonth.parse(påløp.forPeriode),
            )

            val konteringer = oppdragsperiodeMedManglendeKonteringer1.konteringer.plus(oppdragsperiodeMedManglendeKonteringer2.konteringer)
            konteringer.sortedBy { it.overføringsperiode }

            konteringer shouldHaveSize perioderMellomDato.size
            konteringer.shouldBeUnique()
            konteringer.all { it.type == Type.NY.name } shouldBe true
            konteringer.all { it.søknadType == Søknadstype.EN.name } shouldBe true
            konteringer.forEach { it.sendtIPåløpsperiode shouldBe "2023-01" }
            konteringer.forEachIndexed { index, kontering ->
                val periodeForKontering = perioderMellomDato[index]
                kontering.overføringsperiode shouldBe periodeForKontering.toString()
            }
            oppdragsperiodeMedManglendeKonteringer1.konteringerFullførtOpprettet shouldBe true
            oppdragsperiodeMedManglendeKonteringer1.aktivTil shouldNotBe null
            oppdragsperiodeMedManglendeKonteringer2.konteringerFullførtOpprettet shouldBe false
            oppdragsperiodeMedManglendeKonteringer2.aktivTil shouldBe null
        }

    @Test
    fun `Skal opprette konteringer for alle oppdragsperioder som ikke allerede har fått opprettet alle konteringer for en indeksregulering`() =
        runTest {
            val påløp = TestData.opprettPåløp(påløpId = 1, forPeriode = "2023-01")

            val oppdrag = TestData.opprettOppdrag(oppdragsperioder = emptyList())
            val oppdragsperiodeMedManglendeKonteringer = TestData.opprettOppdragsperiode(
                periodeFra = LocalDate.of(2022, 1, 1),
                periodeTil = null,
                konteringer = emptyList(),
                vedtakType = Vedtakstype.INDEKSREGULERING,
                aktivTil = null,
                konteringerFullførtOpprettet = false,
                oppdrag = oppdrag,
            )
            oppdrag.oppdragsperioder = listOf(oppdragsperiodeMedManglendeKonteringer)

            val oppdragsperiodeIder = listOf(
                oppdragsperiodeMedManglendeKonteringer.oppdragsperiodeId,
            )
            every { oppdragsperiodeRepo.hentAlleOppdragsperioderSomIkkeHarOpprettetAlleKonteringer() } returns oppdragsperiodeIder
            every {
                oppdragsperiodeRepo.findById(oppdragsperiodeMedManglendeKonteringer.oppdragsperiodeId)
            } returns Optional.of(oppdragsperiodeMedManglendeKonteringer)

            påløpskjøringService.startPåløpskjøring(påløp, false, true)

            val perioderMellomDato = hentAllePerioderMellomDato(
                oppdragsperiodeMedManglendeKonteringer.periodeFra,
                oppdragsperiodeMedManglendeKonteringer.periodeTil,
                YearMonth.parse(påløp.forPeriode),
            )

            val konteringer = oppdragsperiodeMedManglendeKonteringer.konteringer
            konteringer.sortedBy { it.overføringsperiode }

            konteringer shouldHaveSize perioderMellomDato.size
            konteringer.shouldBeUnique()
            konteringer[0].søknadType shouldBe Søknadstype.IR.name
            konteringer.subList(1, konteringer.size).none { it.søknadType == Søknadstype.IR.name } shouldBe true
            konteringer.all { it.type == Type.NY.name } shouldBe true
            konteringer.subList(1, konteringer.size).all { it.søknadType == Søknadstype.EN.name } shouldBe true
            konteringer.forEach { it.sendtIPåløpsperiode shouldBe "2023-01" }
            konteringer.forEachIndexed { index, kontering ->
                val periodeForKontering = perioderMellomDato[index]
                kontering.overføringsperiode shouldBe periodeForKontering.toString()
            }
        }

    @Test
    fun skalHindreAtKonteringerSkalOpprettesFremITidSelvOmPeriodeTilErSattFremITid() {
        val påløp = TestData.opprettPåløp(påløpId = 1, forPeriode = "2023-01")

        val oppdrag = TestData.opprettOppdrag(oppdragsperioder = emptyList())
        val oppdragsperiodeMedManglendeKonteringer = TestData.opprettOppdragsperiode(
            periodeFra = LocalDate.of(2022, 1, 1),
            periodeTil = LocalDate.of(2024, 1, 1),
            konteringer = emptyList(),
            vedtakType = Vedtakstype.INDEKSREGULERING,
            aktivTil = null,
            konteringerFullførtOpprettet = false,
            oppdrag = oppdrag,
        )

        val perioderMellomDato = hentAllePerioderMellomDato(
            oppdragsperiodeMedManglendeKonteringer.periodeFra,
            oppdragsperiodeMedManglendeKonteringer.periodeTil,
            YearMonth.parse(påløp.forPeriode),
        )

        perioderMellomDato shouldHaveSize 13
    }
}
