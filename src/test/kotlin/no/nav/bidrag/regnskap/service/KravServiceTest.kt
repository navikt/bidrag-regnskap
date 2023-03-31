package no.nav.bidrag.regnskap.service

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.maskinporten.MaskinportenClientException
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
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

        kravService.sendKrav(oppdragId = oppdragsId)

        verify(exactly = 1) { skattConsumer.sendKrav(any()) }
    }

    @Test
    fun `skal sende kontering om perioden kun er for en måned`() {
        every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragForPeriode(
            now,
            now.plusMonths(1)
        )
        every { skattConsumer.sendKrav(any()) } returns ResponseEntity.accepted().body(batchUid)

        kravService.sendKrav(oppdragId = oppdragsId)

        verify(exactly = 1) { skattConsumer.sendKrav(any()) }
    }

    @Test
    fun `skal kaste feil om kontering ikke går igjennom validering`() {
        every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragForPeriode(
            now,
            now.plusMonths(1)
        )
        every { skattConsumer.sendKrav(any()) } returns ResponseEntity.badRequest().body(
            """
          {
            "message": "Tolkning feilet i Elin."
        }
        """
        )
        shouldThrow<HttpClientErrorException> {
            kravService.sendKrav(oppdragId = oppdragsId)
        }
    }

    @Test
    fun `skal kaste feil om tjenesten er slått av`() {
        every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragForPeriode(
            now,
            now.plusMonths(1)
        )
        every { skattConsumer.sendKrav(any()) } returns ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(batchUid)

        shouldThrow<HttpServerErrorException> {
            kravService.sendKrav(oppdragId = oppdragsId)
        }
    }

    @Test
    fun `skal kaste feil om autentisering feiler`() {
        every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragForPeriode(
            now,
            now.plusMonths(1)
        )
        every { skattConsumer.sendKrav(any()) } returns ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(batchUid)

        shouldThrow<MaskinportenClientException> {
            kravService.sendKrav(oppdragId = oppdragsId)
        }
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

        kravService.sendKrav(oppdragId = oppdragsId)

        verify(exactly = 0) { skattConsumer.sendKrav(any()) }
    }

    @Test
    fun `skal ikke sende kontering om oppdrag ikke finnes`() {
        every { persistenceService.hentOppdrag(oppdragsId) } returns null

        shouldThrow<IllegalStateException> {
            kravService.sendKrav(oppdragId = oppdragsId)
        }
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
