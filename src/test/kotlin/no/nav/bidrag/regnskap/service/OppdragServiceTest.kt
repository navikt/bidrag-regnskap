package no.nav.bidrag.regnskap.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OppdragServiceTest {

    @MockK(relaxed = true)
    private lateinit var persistenceService: PersistenceService

    @MockK(relaxed = true)
    private lateinit var oppdragsperiodeService: OppdragsperiodeService

    @MockK(relaxed = true)
    private lateinit var konteringService: KonteringService

    @InjectMockKs
    private lateinit var oppdragService: OppdragService

    @Nested
    inner class OpprettOppdrag {

        @Test
        fun `skal opprette oppdrag`() {
            val hendelse = TestData.opprettHendelse()

            every { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) } returns TestData.opprettOppdragsperiode()

            val oppdragId = oppdragService.lagreEllerOppdaterOppdrag(null, hendelse)

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

            oppdragService.lagreEllerOppdaterOppdrag(oppdrag, hendelse)

            verify { persistenceService.lagreOppdrag(oppdrag) }
        }
    }
}
