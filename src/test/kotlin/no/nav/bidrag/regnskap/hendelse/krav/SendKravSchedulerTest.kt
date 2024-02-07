package no.nav.bidrag.regnskap.hendelse.krav

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.verify
import net.javacrumbs.shedlock.core.LockAssert
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.domene.enums.regnskap.Søknadstype
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.regnskap.hendelse.schedule.krav.KravSchedulerUtils
import no.nav.bidrag.regnskap.hendelse.schedule.krav.SendKravScheduler
import no.nav.bidrag.regnskap.service.KravService
import no.nav.bidrag.regnskap.service.PersistenceService
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
internal class SendKravSchedulerTest {

    @MockK
    private lateinit var persistenceService: PersistenceService

    @MockK
    private lateinit var kravService: KravService

    @MockK
    private lateinit var kravSchedulerUtils: KravSchedulerUtils

    @InjectMockKs
    private lateinit var sendKravScheduler: SendKravScheduler

    @BeforeEach
    fun setup() {
        mockkStatic(LockAssert::class)
        every { LockAssert.assertLocked() } just Runs
    }

    @Test
    fun `skal ikke sende over når det finnes aktivt driftsavvik`() {
        every { kravSchedulerUtils.harAktivtDriftsavvik() } returns true

        sendKravScheduler.skedulertOverforingAvKrav()

        verify(exactly = 0) { kravService.sendKrav(any()) }
    }

    @Test
    fun `skal ikke sende over om vedlikeholdsmodus er påslått`() {
        every { kravSchedulerUtils.harAktivtDriftsavvik() } returns false
        every { kravSchedulerUtils.erVedlikeholdsmodusPåslått() } returns true

        sendKravScheduler.skedulertOverforingAvKrav()

        verify(exactly = 0) { kravService.sendKrav(any()) }
    }

    @Test
    fun `skal ikke sende over om det ikke finnes ikke overførte konteringer`() {
        every { kravSchedulerUtils.harAktivtDriftsavvik() } returns false
        every { kravSchedulerUtils.erVedlikeholdsmodusPåslått() } returns false
        every { persistenceService.hentAlleIkkeOverførteKonteringer() } returns emptyList()
        every { persistenceService.finnSisteOverførtePeriode() } returns YearMonth.now()

        sendKravScheduler.skedulertOverforingAvKrav()

        verify(exactly = 0) { kravService.sendKrav(any()) }
    }

    @Test
    fun `skal sende over kontering`() {
        val oppdrag = TestData.opprettOppdrag(oppdragId = 1)
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag)
        val kontering = TestData.opprettKontering(oppdragsperiode = oppdragsperiode, opprettetTidspunkt = LocalDateTime.now().minusMinutes(1))

        oppdragsperiode.konteringer = listOf(kontering)
        oppdrag.oppdragsperioder = listOf(oppdragsperiode)

        every { kravSchedulerUtils.harAktivtDriftsavvik() } returns false
        every { kravSchedulerUtils.erVedlikeholdsmodusPåslått() } returns false
        every { persistenceService.hentAlleIkkeOverførteKonteringer() } returns listOf(kontering)
        every { persistenceService.finnSisteOverførtePeriode() } returns YearMonth.now()
        every { kravService.sendKrav(any()) } just Runs

        sendKravScheduler.skedulertOverforingAvKrav()

        verify(exactly = 1) { kravService.sendKrav(any()) }
    }

    @Test
    fun `skal filtrere ut oppdrag med fremtidig utsattTilDato`() {
        val oppdrag = TestData.opprettOppdrag(oppdragId = 1, utsattTilDato = LocalDate.now().plusDays(2))
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag)
        val kontering = TestData.opprettKontering(oppdragsperiode = oppdragsperiode, opprettetTidspunkt = LocalDateTime.now().minusMinutes(1))

        oppdragsperiode.konteringer = listOf(kontering)
        oppdrag.oppdragsperioder = listOf(oppdragsperiode)

        every { kravSchedulerUtils.harAktivtDriftsavvik() } returns false
        every { kravSchedulerUtils.erVedlikeholdsmodusPåslått() } returns false
        every { persistenceService.hentAlleIkkeOverførteKonteringer() } returns listOf(kontering)
        every { persistenceService.finnSisteOverførtePeriode() } returns YearMonth.now()

        sendKravScheduler.skedulertOverforingAvKrav()

        verify(exactly = 0) { kravService.sendKrav(any()) }
    }

    @Test
    fun `skal ikke filtrere ut utsattTilDatoer som er passert`() {
        val oppdrag = TestData.opprettOppdrag(oppdragId = 1, utsattTilDato = LocalDate.now())
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag)
        val kontering = TestData.opprettKontering(oppdragsperiode = oppdragsperiode, opprettetTidspunkt = LocalDateTime.now().minusMinutes(1))

        oppdragsperiode.konteringer = listOf(kontering)
        oppdrag.oppdragsperioder = listOf(oppdragsperiode)

        every { kravSchedulerUtils.harAktivtDriftsavvik() } returns false
        every { kravSchedulerUtils.erVedlikeholdsmodusPåslått() } returns false
        every { persistenceService.hentAlleIkkeOverførteKonteringer() } returns listOf(kontering)
        every { persistenceService.finnSisteOverførtePeriode() } returns YearMonth.now()
        every { kravService.sendKrav(any()) } just Runs

        sendKravScheduler.skedulertOverforingAvKrav()

        verify(exactly = 1) { kravService.sendKrav(any()) }
    }

    @Test
    fun `skal sende over flere oppdrag med samme sakId i samme krav`() {
        val bm = PersonidentGenerator.genererFødselsnummer()
        val bp = PersonidentGenerator.genererFødselsnummer()
        val barn = PersonidentGenerator.genererFødselsnummer()
        val nav = "80000345435"

        val annetOppdrag = TestData.opprettOppdrag(oppdragId = 0, sakId = "654321")

        val bidragOppdrag = TestData.opprettOppdrag(
            oppdragId = 1,
            skyldnerIdent = bp,
            kravhaverIdent = bm,
            gjelderIdent = barn,
            mottakerIdent = bm,
            sakId = "123456",
        )

        val gebyrBpOppdrag = TestData.opprettOppdrag(
            stonadType = null,
            engangsbelopType = Engangsbeløptype.GEBYR_SKYLDNER,
            oppdragId = 2,
            skyldnerIdent = bp,
            kravhaverIdent = nav,
            gjelderIdent = bp,
            mottakerIdent = nav,
            sakId = "123456",
        )
        val gebyrBmOppdrag = TestData.opprettOppdrag(
            stonadType = null,
            engangsbelopType = Engangsbeløptype.GEBYR_MOTTAKER,
            oppdragId = 3,
            skyldnerIdent = bm,
            kravhaverIdent = nav,
            gjelderIdent = bm,
            mottakerIdent = nav,
            sakId = "123456",
        )

        val annenOppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = annetOppdrag, oppdragsperiodeId = 0)

        val bidragOppdragsperiode = TestData.opprettOppdragsperiode(
            oppdrag = bidragOppdrag,
            oppdragsperiodeId = 1,
            periodeTil = null,
        )
        val gebyrBpOppdragsperiode = TestData.opprettOppdragsperiode(
            oppdrag = gebyrBpOppdrag,
            oppdragsperiodeId = 2,
            periodeFra = LocalDate.now(),
        )
        val gebyrBmOppdragsperiode = TestData.opprettOppdragsperiode(
            oppdrag = gebyrBmOppdrag,
            oppdragsperiodeId = 3,
            periodeFra = LocalDate.now(),
        )

        val annenKontering = TestData.opprettKontering(
            oppdragsperiode = annenOppdragsperiode,
            konteringId = 0,
            opprettetTidspunkt = LocalDateTime.now().minusMinutes(1),
        )

        val bidragKontering = TestData.opprettKontering(
            oppdragsperiode = bidragOppdragsperiode,
            konteringId = 1,
            transaksjonskode = Transaksjonskode.B1.name,
            opprettetTidspunkt = LocalDateTime.now().minusMinutes(1),
        )
        val gebyrBpKontering = TestData.opprettKontering(
            oppdragsperiode = gebyrBpOppdragsperiode,
            konteringId = 2,
            transaksjonskode = Transaksjonskode.G1.name,
            søknadstype = Søknadstype.FABP.name,
            opprettetTidspunkt = LocalDateTime.now().minusMinutes(1),
        )
        val gebyrBmKontering = TestData.opprettKontering(
            oppdragsperiode = gebyrBmOppdragsperiode,
            konteringId = 3,
            transaksjonskode = Transaksjonskode.G1.name,
            søknadstype = Søknadstype.FABM.name,
            opprettetTidspunkt = LocalDateTime.now().minusMinutes(1),
        )

        gebyrBmOppdragsperiode.konteringer = listOf(gebyrBmKontering)
        gebyrBpOppdragsperiode.konteringer = listOf(gebyrBpKontering)
        bidragOppdragsperiode.konteringer = listOf(bidragKontering)
        annenOppdragsperiode.konteringer = listOf(annenKontering)

        gebyrBmOppdrag.oppdragsperioder = listOf(gebyrBmOppdragsperiode)
        gebyrBpOppdrag.oppdragsperioder = listOf(gebyrBpOppdragsperiode)
        bidragOppdrag.oppdragsperioder = listOf(bidragOppdragsperiode)
        annetOppdrag.oppdragsperioder = listOf(annenOppdragsperiode)

        every { kravSchedulerUtils.harAktivtDriftsavvik() } returns false
        every { kravSchedulerUtils.erVedlikeholdsmodusPåslått() } returns false
        every { persistenceService.hentAlleIkkeOverførteKonteringer() } returns listOf(
            annenKontering,
            bidragKontering,
            gebyrBpKontering,
            gebyrBmKontering,
        )

        every { kravService.sendKrav(any()) } just Runs

        sendKravScheduler.skedulertOverforingAvKrav()

        verify(exactly = 2) { kravService.sendKrav(any()) }
    }

    @Test
    fun `skal ikke sende over krav som er opprettet innen siste 30 sekunder for å unngå dobbel oversending`() {
        val oppdrag = TestData.opprettOppdrag(oppdragId = 1)
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag)
        val kontering = TestData.opprettKontering(oppdragsperiode = oppdragsperiode)

        every { kravSchedulerUtils.harAktivtDriftsavvik() } returns false
        every { kravSchedulerUtils.erVedlikeholdsmodusPåslått() } returns false
        every { persistenceService.hentAlleIkkeOverførteKonteringer() } returns listOf(kontering)
        every { persistenceService.finnSisteOverførtePeriode() } returns YearMonth.now()

        sendKravScheduler.skedulertOverforingAvKrav()

        verify(exactly = 0) { kravService.sendKrav(any()) }
    }
}
