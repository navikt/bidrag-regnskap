package no.nav.bidrag.regnskap.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.regnskap.consumer.SakConsumer
import no.nav.bidrag.regnskap.util.IdentUtils
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OppdragServiceTest {

    @MockK(relaxed = true)
    private lateinit var persistenceService: PersistenceService

    @MockK(relaxed = true)
    private lateinit var oppdragsperiodeService: OppdragsperiodeService

    @MockK(relaxed = true)
    private lateinit var konteringService: KonteringService

    @MockK(relaxed = true)
    private lateinit var sakConsumer: SakConsumer

    @InjectMockKs
    private lateinit var oppdragService: OppdragService

    @Nested
    inner class OpprettOppdrag {

        @Test
        fun `skal opprette oppdrag`() {
            val hendelse = TestData.opprettHendelse()

            every { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) } returns TestData.opprettOppdragsperiode()

            val oppdragId = oppdragService.lagreEllerOppdaterOppdrag(null, hendelse, false)

            oppdragId shouldBe 0
        }
    }

    @Nested
    inner class OppdaterOppdrag {

        @Test
        fun `skal oppdatere oppdrag`() {
            val hendelse = TestData.opprettHendelse()
            val oppdrag = TestData.opprettOppdrag()

            every { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) } returns TestData.opprettOppdragsperiode()

            oppdragService.lagreEllerOppdaterOppdrag(oppdrag, hendelse, false)

            verify { persistenceService.lagreOppdrag(oppdrag) }
        }

        @Test
        fun `skal oppdatere utsatt til dato om ikke satt på oppdrag`() {
            val utsattTilDato = LocalDate.now().plusDays(3)
            val hendelse = TestData.opprettHendelse(utsattTilDato = utsattTilDato)
            val oppdrag = TestData.opprettOppdrag(utsattTilDato = null)

            oppdragService.oppdatererVerdierPåOppdrag(hendelse, oppdrag)

            oppdrag.utsattTilDato shouldBe utsattTilDato
        }

        @Test
        fun `skal oppdatere utsatt til dato om satt på oppdrag men hendelse er lenger frem i tid`() {
            val utsattTilDato = LocalDate.now().plusDays(3)
            val hendelse = TestData.opprettHendelse(utsattTilDato = utsattTilDato)
            val oppdrag = TestData.opprettOppdrag(utsattTilDato = utsattTilDato.minusDays(1))

            oppdragService.oppdatererVerdierPåOppdrag(hendelse, oppdrag)

            oppdrag.utsattTilDato shouldBe utsattTilDato
        }

        @Test
        fun `skal ikke oppdatere utsatt til dato om satt på oppdrag og hendelse er tilbake i tid`() {
            val utsattTilDato = LocalDate.now().plusDays(3)
            val hendelse = TestData.opprettHendelse(utsattTilDato = utsattTilDato)
            val oppdrag = TestData.opprettOppdrag(utsattTilDato = utsattTilDato.plusDays(1))

            oppdragService.oppdatererVerdierPåOppdrag(hendelse, oppdrag)

            oppdrag.utsattTilDato shouldBe utsattTilDato.plusDays(1)
        }

        @Test
        fun `skal ikke oppdatere utsatt til dato om satt på oppdrag og hendelse er null`() {
            val utsattTilDato = LocalDate.now().plusDays(3)
            val hendelse = TestData.opprettHendelse(utsattTilDato = null)
            val oppdrag = TestData.opprettOppdrag(utsattTilDato = utsattTilDato)

            oppdragService.oppdatererVerdierPåOppdrag(hendelse, oppdrag)

            oppdrag.utsattTilDato shouldBe utsattTilDato
        }

        @Test
        fun `skal ikke oppdatere utsatt til dato null på oppdrag og hendelse er null`() {
            val hendelse = TestData.opprettHendelse(utsattTilDato = null)
            val oppdrag = TestData.opprettOppdrag(utsattTilDato = null)

            oppdragService.oppdatererVerdierPåOppdrag(hendelse, oppdrag)

            oppdrag.utsattTilDato shouldBe null
        }
    }

    @Nested
    inner class PatchMottaker {

        @Test
        fun `skal patche mottaker når det finnes et bidrags oppdrag`() {
            val saksnummer = Saksnummer("123456")
            val kravhaver = Personident(PersonidentGenerator.genererFødselsnummer())
            val mottaker = Personident(PersonidentGenerator.genererFødselsnummer())

            val oppdrag = TestData.opprettOppdrag(
                stonadType = Stønadstype.BIDRAG,
                sakId = saksnummer.verdi,
                kravhaverIdent = kravhaver.verdi,
                mottakerIdent = "DUMMY",
            )
            val oppdragsperiode1 = TestData.opprettOppdragsperiode(oppdrag = oppdrag)
            val oppdragsperiode2 = TestData.opprettOppdragsperiode(oppdrag = oppdrag)
            oppdrag.oppdragsperioder = listOf(oppdragsperiode1, oppdragsperiode2)

            every { persistenceService.hentOppdragPåSaksnummerOgKravhaver(saksnummer, kravhaver) } returns listOf(oppdrag)

            oppdragService.patchMottaker(saksnummer, kravhaver, mottaker)

            oppdrag.mottakerIdent shouldBe mottaker.verdi
        }

        @Test
        fun `skal patche mottaker når det finnes et bidrag- og et forskudds oppdrag`() {
            val saksnummer = Saksnummer("123456")
            val kravhaver = Personident(PersonidentGenerator.genererFødselsnummer())
            val mottaker = Personident(PersonidentGenerator.genererFødselsnummer())

            val oppdrag1 = TestData.opprettOppdrag(
                stonadType = Stønadstype.BIDRAG,
                sakId = saksnummer.verdi,
                kravhaverIdent = kravhaver.verdi,
                mottakerIdent = "DUMMY",
            )
            val oppdragsperiode1 = TestData.opprettOppdragsperiode(oppdrag = oppdrag1)
            val oppdragsperiode2 = TestData.opprettOppdragsperiode(oppdrag = oppdrag1)
            oppdrag1.oppdragsperioder = listOf(oppdragsperiode1, oppdragsperiode2)

            val oppdrag2 = TestData.opprettOppdrag(
                stonadType = Stønadstype.FORSKUDD,
                sakId = saksnummer.verdi,
                kravhaverIdent = kravhaver.verdi,
                mottakerIdent = "DUMMY",
            )
            val oppdragsperiode3 = TestData.opprettOppdragsperiode(oppdrag = oppdrag2)
            val oppdragsperiode4 = TestData.opprettOppdragsperiode(oppdrag = oppdrag2)
            oppdrag2.oppdragsperioder = listOf(oppdragsperiode3, oppdragsperiode4)

            every { persistenceService.hentOppdragPåSaksnummerOgKravhaver(saksnummer, kravhaver) } returns listOf(oppdrag1, oppdrag2)

            oppdragService.patchMottaker(saksnummer, kravhaver, mottaker)

            oppdrag1.mottakerIdent shouldBe mottaker.verdi
            oppdrag2.mottakerIdent shouldBe mottaker.verdi
        }

        @Test
        fun `skal patche mottaker for gebyr`() {
            val saksnummer = Saksnummer("123456")
            val kravhaver = Personident(PersonidentGenerator.genererFødselsnummer())
            val mottaker = Personident(PersonidentGenerator.genererFødselsnummer())

            val oppdrag1 = TestData.opprettOppdrag(
                stonadType = null,
                engangsbelopType = Engangsbeløptype.GEBYR_MOTTAKER,
                sakId = saksnummer.verdi,
                kravhaverIdent = kravhaver.verdi,
                mottakerIdent = "DUMMY",
            )
            val oppdragsperiode1 = TestData.opprettOppdragsperiode(oppdrag = oppdrag1)
            oppdrag1.oppdragsperioder = listOf(oppdragsperiode1)

            every { persistenceService.hentOppdragPåSaksnummerOgKravhaver(saksnummer, kravhaver) } returns listOf(oppdrag1)

            oppdragService.patchMottaker(saksnummer, kravhaver, mottaker)

            oppdrag1.mottakerIdent shouldBe IdentUtils.NAV_TSS_IDENT
        }

        @Test
        fun `skal patche mottaker for ektefellebidrag`() {
            val saksnummer = Saksnummer("123456")
            val kravhaver = Personident(PersonidentGenerator.genererFødselsnummer())
            val mottaker = Personident(PersonidentGenerator.genererFødselsnummer())

            val oppdrag1 = TestData.opprettOppdrag(
                stonadType = Stønadstype.EKTEFELLEBIDRAG,
                sakId = saksnummer.verdi,
                kravhaverIdent = kravhaver.verdi,
                mottakerIdent = "DUMMY",
            )
            val oppdragsperiode1 = TestData.opprettOppdragsperiode(oppdrag = oppdrag1)
            oppdrag1.oppdragsperioder = listOf(oppdragsperiode1)

            every { persistenceService.hentOppdragPåSaksnummerOgKravhaver(saksnummer, kravhaver) } returns listOf(oppdrag1)

            oppdragService.patchMottaker(saksnummer, kravhaver, mottaker)

            oppdrag1.mottakerIdent shouldBe oppdrag1.kravhaverIdent
        }
    }
}
