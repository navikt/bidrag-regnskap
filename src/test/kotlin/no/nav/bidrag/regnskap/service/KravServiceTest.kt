package no.nav.bidrag.regnskap.service

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.domene.enums.regnskap.Søknadstype
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class KravServiceTest {

    @MockK(relaxed = true)
    private lateinit var persistenceService: PersistenceService

    @MockK(relaxed = true)
    private lateinit var skattConsumer: SkattConsumer

    @MockK(relaxed = true)
    private lateinit var behandlingsstatusService: BehandlingsstatusService

    @InjectMockKs
    private lateinit var kravService: KravService

    val oppdragsId = 1
    val now = LocalDate.now()
    val batchUid = "{\"BatchUid\":\"asijdk-32546s-jhsjhs\", \"ValidationMessages\":[]}"

    @Test
    fun `skal sende kontering til skatt når oppdragsperioden er innenfor innsendt periode`() {
        every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragForPeriode(
            now.minusMonths(3),
            now.plusMonths(1),
        )
        every { skattConsumer.sendKrav(any()) } returns ResponseEntity.accepted().body(batchUid)

        kravService.sendKrav(listOf(oppdragsId))

        verify(exactly = 1) { skattConsumer.sendKrav(any()) }
    }

    @Test
    fun `skal sende kontering om perioden kun er for en måned`() {
        every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragForPeriode(
            now,
            now.plusMonths(1),
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
                    konteringer = listOf(TestData.opprettKontering(overforingstidspunkt = LocalDateTime.now())),
                ),
            ),
        )

        kravService.sendKrav(listOf(oppdragsId))

        verify(exactly = 0) { skattConsumer.sendKrav(any()) }
    }

    @Test
    fun `skal ikke sende konteringer som er utsatt`() {
        every { persistenceService.hentOppdrag(oppdragsId) } returns TestData.opprettOppdrag(
            utsattTilDato = LocalDate.now().plusDays(1),
        )

        kravService.sendKrav(listOf(oppdragsId))

        verify(exactly = 0) { skattConsumer.sendKrav(any()) }
    }

    @Test
    fun `skal sende konteringer hvor utsatt dato er passert`() {
        val oppdrag = opprettOppdragForPeriode(
            now.minusMonths(3),
            now.plusMonths(1),
        )
        oppdrag.utsattTilDato = LocalDate.now().minusDays(1)

        every { persistenceService.hentOppdrag(oppdragsId) } returns oppdrag
        every { skattConsumer.sendKrav(any()) } returns ResponseEntity.accepted().body(batchUid)

        kravService.sendKrav(listOf(oppdragsId))

        verify(exactly = 1) { skattConsumer.sendKrav(any()) }
    }

    @Test
    fun `skal ikke sende konteringer hvor det finnes ikke godkjente overføringer`() {
        val oppdrag = opprettOppdragForPeriode(
            now.minusMonths(3),
            now.plusMonths(1),
        )
        val refernsekode = "test"

        oppdrag.oppdragsperioder.first().konteringer.first().sisteReferansekode = refernsekode

        every { persistenceService.hentOppdrag(oppdragsId) } returns oppdrag
        every {
            behandlingsstatusService.hentBehandlingsstatusForIkkeGodkjenteKonteringerForReferansekode(listOf(refernsekode))
        } returns hashMapOf(Pair("batchUid", "FEIL"), Pair("batchuid2", "FEIL2"))

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
        val bm = PersonidentGenerator.genererFødselsnummer()
        val bp = PersonidentGenerator.genererFødselsnummer()
        val barn = PersonidentGenerator.genererFødselsnummer()
        val nav = "80000345435"

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

        val bidragKontering = TestData.opprettKontering(
            oppdragsperiode = bidragOppdragsperiode,
            konteringId = 1,
            transaksjonskode = Transaksjonskode.B1.name,
        )
        val gebyrBpKontering = TestData.opprettKontering(
            oppdragsperiode = gebyrBpOppdragsperiode,
            konteringId = 2,
            transaksjonskode = Transaksjonskode.G1.name,
            søknadstype = Søknadstype.FABP.name,
        )
        val gebyrBmKontering = TestData.opprettKontering(
            oppdragsperiode = gebyrBmOppdragsperiode,
            konteringId = 3,
            transaksjonskode = Transaksjonskode.G1.name,
            søknadstype = Søknadstype.FABM.name,
        )

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

    @Test
    fun `skal sende samme oppdragId men forskjellig vedtakId i forskjellige Krav i samme kall mot skatt`() {
        val bm = PersonidentGenerator.genererFødselsnummer()
        val bp = PersonidentGenerator.genererFødselsnummer()
        val barn = PersonidentGenerator.genererFødselsnummer()
        val nav = "80000345435"

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

        val bidragOppdragsperiode = TestData.opprettOppdragsperiode(
            oppdrag = bidragOppdrag,
            vedtakId = 123456,
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

        val bidragKontering = TestData.opprettKontering(
            oppdragsperiode = bidragOppdragsperiode,
            konteringId = 1,
            transaksjonskode = Transaksjonskode.B1.name,
        )
        val gebyrBpKontering = TestData.opprettKontering(
            oppdragsperiode = gebyrBpOppdragsperiode,
            konteringId = 2,
            transaksjonskode = Transaksjonskode.G1.name,
            søknadstype = Søknadstype.FABP.name,
        )
        val gebyrBmKontering = TestData.opprettKontering(
            oppdragsperiode = gebyrBmOppdragsperiode,
            konteringId = 3,
            transaksjonskode = Transaksjonskode.G1.name,
            søknadstype = Søknadstype.FABM.name,
        )

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

    @Test
    fun `skal opprette kravliste sortert med eldste vedtak først`() {
        val bm = PersonidentGenerator.genererFødselsnummer()
        val bp = PersonidentGenerator.genererFødselsnummer()
        val barn = PersonidentGenerator.genererFødselsnummer()
        val nav = "80000345435"

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

        val bidragOppdragsperiode = TestData.opprettOppdragsperiode(
            oppdrag = bidragOppdrag,
            vedtakId = 123456,
            oppdragsperiodeId = 1,
            periodeTil = null,
        )
        val gebyrBpOppdragsperiode = TestData.opprettOppdragsperiode(
            oppdrag = gebyrBpOppdrag,
            vedtakId = 12345,
            oppdragsperiodeId = 2,
            periodeFra = LocalDate.now(),
        )
        val gebyrBmOppdragsperiode = TestData.opprettOppdragsperiode(
            oppdrag = gebyrBmOppdrag,
            vedtakId = 1234,
            oppdragsperiodeId = 3,
            periodeFra = LocalDate.now(),
        )

        val bidragKontering = TestData.opprettKontering(
            oppdragsperiode = bidragOppdragsperiode,
            konteringId = 1,
            transaksjonskode = Transaksjonskode.B1.name,
            vedtakId = 3,
        )
        val gebyrBpKontering = TestData.opprettKontering(
            oppdragsperiode = gebyrBpOppdragsperiode,
            konteringId = 2,
            transaksjonskode = Transaksjonskode.G1.name,
            søknadstype = Søknadstype.FABP.name,
            vedtakId = 2,
        )
        val gebyrBmKontering = TestData.opprettKontering(
            oppdragsperiode = gebyrBmOppdragsperiode,
            konteringId = 3,
            transaksjonskode = Transaksjonskode.G1.name,
            søknadstype = Søknadstype.FABM.name,
            vedtakId = 1,
        )

        bidragOppdragsperiode.konteringer = listOf(bidragKontering)
        gebyrBpOppdragsperiode.konteringer = listOf(gebyrBpKontering)
        gebyrBmOppdragsperiode.konteringer = listOf(gebyrBmKontering)

        bidragOppdrag.oppdragsperioder = listOf(bidragOppdragsperiode)
        gebyrBpOppdrag.oppdragsperioder = listOf(gebyrBpOppdragsperiode)
        gebyrBmOppdrag.oppdragsperioder = listOf(gebyrBmOppdragsperiode)

        val oppdragsperioderMedIkkeOverførteKonteringerListe = listOf(bidragOppdrag, gebyrBmOppdrag, gebyrBpOppdrag).flatMap {
            kravService.hentOppdragsperioderMedIkkeOverførteKonteringer(it)
        }

        val kravlister = kravService.opprettKravlister(oppdragsperioderMedIkkeOverførteKonteringerListe)

        kravlister shouldHaveSize 3
        kravlister[0].krav[0].konteringer[0].soknadType shouldBe Søknadstype.FABM
        kravlister[1].krav[0].konteringer[0].soknadType shouldBe Søknadstype.FABP
        kravlister[2].krav[0].konteringer[0].soknadType shouldBe Søknadstype.EN
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
                                oppdrag = TestData.opprettOppdrag(),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}
