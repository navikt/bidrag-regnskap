package no.nav.bidrag.regnskap.service

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.behandling.felles.enums.EngangsbelopType
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.dto.enumer.Søknadstype
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class KravServiceTest {

    @MockK(relaxed = true)
    private lateinit var persistenceService: PersistenceService

    @MockK(relaxed = true)
    private lateinit var skattConsumer: SkattConsumer

    @InjectMockKs
    private lateinit var kravService: KravService

    val oppdragsId = 1
    val now = LocalDate.now()
    val batchUid = "{\"BatchUid\":\"asijdk-32546s-jhsjhs\", \"ValidationMessages\":[]}"

    @Test
    fun `skal sende kontering til skatt når oppdragsperioden er innenfor innsendt periode`() {
        every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragForPeriode(
            now.minusMonths(3),
            now.plusMonths(1)
        )
        every { skattConsumer.sendKrav(any()) } returns ResponseEntity.accepted().body(batchUid)

        kravService.sendKrav(listOf(oppdragsId))

        verify(exactly = 1) { skattConsumer.sendKrav(any()) }
    }

    @Test
    fun `skal sende kontering om perioden kun er for en måned`() {
        every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragForPeriode(
            now,
            now.plusMonths(1)
        )
        every { skattConsumer.sendKrav(any()) } returns ResponseEntity.accepted().body(batchUid)

        kravService.sendKrav(listOf(oppdragsId))

        verify(exactly = 1) { skattConsumer.sendKrav(any()) }
    }

    @Test
    fun `skal ikke sende kontering til skatt når kontering allerede er overført`() {
        every { persistenceService.hentOppdrag(oppdragsId) } returns TestData.opprettOppdrag(
            oppdragsperioder = listOf(
                TestData.opprettOppdragsperiode(
                    konteringer = listOf(TestData.opprettKontering(overforingstidspunkt = LocalDateTime.now()))
                )
            )
        )

        kravService.sendKrav(listOf(oppdragsId))

        verify(exactly = 0) { skattConsumer.sendKrav(any()) }
    }

    @Test
    fun `skal ikke sende kontering om oppdrag ikke finnes`() {
        every { persistenceService.hentOppdrag(oppdragsId) } returns null

        shouldNotThrowAny {
            kravService.sendKrav(listOf(oppdragsId))
        }
        verify(exactly = 0) { skattConsumer.sendKrav(any()) }
    }

    @Test
    fun `skal støtte å sende over flere oppdrag i samme krav`() {
        val bm = PersonidentGenerator.genererPersonnummer()
        val bp = PersonidentGenerator.genererPersonnummer()
        val barn = PersonidentGenerator.genererPersonnummer()
        val nav = "80000345435"

        val bidragOppdrag = TestData.opprettOppdrag(oppdragId = 1, skyldnerIdent = bp, kravhaverIdent = bm, sakId = "123456")
        val gebyrBpOppdrag = TestData.opprettOppdrag(stonadType = null, engangsbelopType = EngangsbelopType.GEBYR_SKYLDNER, oppdragId = 2, skyldnerIdent = bp, kravhaverIdent = nav, sakId = "123456")
        val gebyrBmOppdrag = TestData.opprettOppdrag(stonadType = null, engangsbelopType = EngangsbelopType.GEBYR_MOTTAKER, oppdragId = 3, skyldnerIdent = bm, kravhaverIdent = nav, sakId = "123456")

        val bidragOppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = bidragOppdrag, oppdragsperiodeId = 1, gjelderIdent = barn, mottakerIdent = bm, periodeTil = null)
        val gebyrBpOppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = gebyrBpOppdrag, oppdragsperiodeId = 2, gjelderIdent = bp, mottakerIdent = nav, periodeFra = LocalDate.now())
        val gebyrBmOppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = gebyrBmOppdrag, oppdragsperiodeId = 3, gjelderIdent = bm, mottakerIdent = nav, periodeFra = LocalDate.now())

        val bidragKontering = TestData.opprettKontering(oppdragsperiode = bidragOppdragsperiode, konteringId = 1, transaksjonskode = Transaksjonskode.B1.name)
        val gebyrBpKontering = TestData.opprettKontering(oppdragsperiode = gebyrBpOppdragsperiode, konteringId = 2, transaksjonskode = Transaksjonskode.G1.name, søknadstype = Søknadstype.FABP.name)
        val gebyrBmKontering = TestData.opprettKontering(oppdragsperiode = gebyrBmOppdragsperiode, konteringId = 3, transaksjonskode = Transaksjonskode.G1.name, søknadstype = Søknadstype.FABM.name)

        bidragOppdragsperiode.konteringer = listOf(bidragKontering)
        gebyrBpOppdragsperiode.konteringer = listOf(gebyrBpKontering)
        gebyrBmOppdragsperiode.konteringer = listOf(gebyrBmKontering)

        bidragOppdrag.oppdragsperioder = listOf(bidragOppdragsperiode)
        gebyrBpOppdrag.oppdragsperioder = listOf(gebyrBpOppdragsperiode)
        gebyrBmOppdrag.oppdragsperioder = listOf(gebyrBmOppdragsperiode)

        every { persistenceService.hentOppdrag(1) } returns bidragOppdrag
        every { persistenceService.hentOppdrag(2) } returns gebyrBpOppdrag
        every { persistenceService.hentOppdrag(3) } returns gebyrBmOppdrag

        kravService.sendKrav(listOf(1, 2, 3))

        verify(exactly = 1) { skattConsumer.sendKrav(any()) }
    }

    private fun opprettOppdragForPeriode(periodeFra: LocalDate, periodeTil: LocalDate): Oppdrag {
        return TestData.opprettOppdrag(
            oppdragsperioder = listOf(
                TestData.opprettOppdragsperiode(
                    periodeTil = periodeTil,
                    periodeFra = periodeFra,
                    konteringer = listOf(
                        TestData.opprettKontering(
                            oppdragsperiode = TestData.opprettOppdragsperiode(
                                oppdrag = TestData.opprettOppdrag()
                            )
                        )
                    )
                )
            )
        )
    }
}
