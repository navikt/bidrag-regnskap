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
import no.nav.bidrag.regnskap.hendelse.schedule.krav.KravSchedulerUtils
import no.nav.bidrag.regnskap.hendelse.schedule.krav.SendKravScheduler
import no.nav.bidrag.regnskap.service.KravService
import no.nav.bidrag.regnskap.service.PersistenceService
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
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
        val kontering = TestData.opprettKontering(oppdragsperiode = oppdragsperiode)

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
        val kontering = TestData.opprettKontering(oppdragsperiode = oppdragsperiode)

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
        val kontering = TestData.opprettKontering(oppdragsperiode = oppdragsperiode)

        every { kravSchedulerUtils.harAktivtDriftsavvik() } returns false
        every { kravSchedulerUtils.erVedlikeholdsmodusPåslått() } returns false
        every { persistenceService.hentAlleIkkeOverførteKonteringer() } returns listOf(kontering)
        every { persistenceService.finnSisteOverførtePeriode() } returns YearMonth.now()
        every { kravService.sendKrav(any()) } just Runs

        sendKravScheduler.skedulertOverforingAvKrav()

        verify(exactly = 1) { kravService.sendKrav(any()) }
    }
}
