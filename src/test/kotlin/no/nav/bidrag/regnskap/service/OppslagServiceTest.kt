package no.nav.bidrag.regnskap.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.domene.enums.Stønadstype
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.domene.enums.regnskap.Type
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class OppslagServiceTest {

    @MockK(relaxed = true)
    private lateinit var persistenceService: PersistenceService

    @InjectMockKs
    private lateinit var oppslagService: OppslagService

    @Nested
    inner class HentOppdrag {

        @Test
        fun `skal hente eksisterende oppdrag`() {
            val stonadType = Stønadstype.BIDRAG
            val skyldnerIdent = PersonidentGenerator.genererFødselsnummer()

            every { persistenceService.hentOppdrag(any()) } returns TestData.opprettOppdrag(
                stonadType = stonadType,
                skyldnerIdent = skyldnerIdent,
            )

            val oppdragResponse = oppslagService.hentOppdrag(1)!!

            oppdragResponse.type shouldBe stonadType.name
            oppdragResponse.skyldnerIdent shouldBe skyldnerIdent
        }

        @Test
        fun `skal være tom om oppdrag ikke eksisterer`() {
            every { persistenceService.hentOppdrag(any()) } returns null
            oppslagService.hentOppdrag(1) shouldBe null
        }
    }

    @Nested
    inner class HentOppdragsperioder {

        @Test
        fun `Skal hente oppdragsperiode`() {
            val oppdrag = TestData.opprettOppdrag()
            val opprettetOppdragsperiodeListe = oppslagService.hentOppdragsperioderMedKonteringer(oppdrag)

            opprettetOppdragsperiodeListe shouldHaveSize oppdrag.oppdragsperioder.size
        }
    }

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
                                type = Type.NY.name,
                            ),
                            TestData.opprettKontering(
                                konteringId = 2,
                                transaksjonskode = transaksjonskode.toString(),
                                overforingsperiode = overforingsperiode.plusMonths(1).toString(),
                                type = Type.ENDRING.name,
                            ),
                        ),
                    ),
                ),
            )

            val konteringResponseListe = oppslagService.hentKonteringer(oppdrag.oppdragsperioder.first())

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
    inner class HentPåSakId
}
