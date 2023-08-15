package no.nav.bidrag.regnskap.fil.påløp

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.fil.overføring.FiloverføringTilElinKlient
import no.nav.bidrag.regnskap.persistence.bucket.GcpFilBucket
import no.nav.bidrag.regnskap.service.PersistenceService
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class PåløpsfilGeneratorTest {

    @MockK(relaxed = true)
    private lateinit var gcpFilBucket: GcpFilBucket

    @MockK(relaxed = true)
    private lateinit var persistenceService: PersistenceService

    @MockK(relaxed = true)
    private lateinit var filoverføringTilElinKlient: FiloverføringTilElinKlient

    @InjectMockKs
    private lateinit var påløpsfilGenerator: PåløpsfilGenerator

    @Test
    fun `skal skrive påløpsfil`() = runTest {
        val oppdrag = TestData.opprettOppdrag(oppdragId = 1)
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag)
        val kontering1 = TestData.opprettKontering(
            konteringId = 1,
            transaksjonskode = Transaksjonskode.A1.toString(),
            oppdragsperiode = oppdragsperiode
        )
        val kontering2 = TestData.opprettKontering(
            konteringId = 2,
            transaksjonskode = Transaksjonskode.B1.toString(),
            oppdragsperiode = oppdragsperiode
        )
        val konteringer = listOf(kontering1, kontering2)
        oppdrag.oppdragsperioder = listOf(oppdragsperiode)
        oppdragsperiode.konteringer = konteringer

        val oppdrag2 = TestData.opprettOppdrag(oppdragId = 2)
        val oppdragsperiode2 = TestData.opprettOppdragsperiode(oppdrag = oppdrag2)
        val kontering3 = TestData.opprettKontering(
            konteringId = 3,
            transaksjonskode = Transaksjonskode.H1.toString(),
            oppdragsperiode = oppdragsperiode2
        )
        val konteringer2 = listOf(kontering3)
        oppdrag2.oppdragsperioder = listOf(oppdragsperiode2)
        oppdragsperiode2.konteringer = konteringer2

        val påløp = TestData.opprettPåløp()

        every { persistenceService.hentAlleIkkeOverførteKonteringer() } returns konteringer + konteringer2

        påløpsfilGenerator.skrivPåløpsfilOgLastOppPåFilsluse(påløp, emptyList())
    }
}
