package no.nav.bidrag.regnskap.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
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
  val batchUid = "{\"batchUid\":\"asijdk-32546s-jhsjhs\"}"


  @Test
  fun `skal sende kontering til skatt når oppdragsperioden er innenfor innsendt periode`() {
    every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragOptionalForPeriode(
      now.minusMonths(3), now.plusMonths(1)
    )
    every { skattConsumer.sendKrav(any()) } returns ResponseEntity.accepted().body(batchUid)

    val restResponse = kravService.sendKrav(oppdragId = oppdragsId, YearMonth.of(now.year, now.month))

    verify(exactly = 1) { skattConsumer.sendKrav(any()) }

    restResponse.statusCode shouldBe HttpStatus.OK
  }

  @Test
  fun `skal sende kontering om perioden kun er for en måned`() {
    every { persistenceService.hentOppdrag(oppdragsId) } returns opprettOppdragOptionalForPeriode(
      now, now.plusMonths(1)
    )
    every { skattConsumer.sendKrav(any()) } returns ResponseEntity.accepted().body(batchUid)

    val restResponse = kravService.sendKrav(oppdragId = oppdragsId, YearMonth.of(now.year, now.month))

    restResponse.statusCode shouldBe HttpStatus.OK
  }

  @Test
  fun `skal ikke sende kontering til skatt når kontering allerede er overført`() {
    every { persistenceService.hentOppdrag(oppdragsId) } returns Optional.of(
      TestData.opprettOppdrag(
        oppdragsperioder = listOf(
          TestData.opprettOppdragsperiode(
            konteringer = listOf(
              TestData.opprettKontering(
                overforingstidspunkt = LocalDateTime.now()
              )
            )
          )
        )
      )
    )

    val restResponse = kravService.sendKrav(oppdragId = oppdragsId, YearMonth.of(now.year, now.month))

    restResponse.statusCode shouldBe HttpStatus.NO_CONTENT
  }

  @Test
  fun `skal ikke sende kontering om oppdrag ikke finnes`() {
    every { persistenceService.hentOppdrag(oppdragsId) } returns Optional.empty()

    shouldThrow<NoSuchElementException> {
      kravService.sendKrav(oppdragId = oppdragsId, YearMonth.of(now.year, now.month))
    }
  }

  private fun opprettOppdragOptionalForPeriode(periodeFra: LocalDate, periodeTil: LocalDate): Optional<Oppdrag> {
    return Optional.of(
      TestData.opprettOppdrag(
        oppdragsperioder = listOf(
          TestData.opprettOppdragsperiode(
            periodeTil = periodeTil, periodeFra = periodeFra, konteringer = listOf(
              TestData.opprettKontering(
                oppdragsperiode = TestData.opprettOppdragsperiode(
                  oppdrag = TestData.opprettOppdrag()
                )
              )
            )
          )
        )
      )
    )
  }
}