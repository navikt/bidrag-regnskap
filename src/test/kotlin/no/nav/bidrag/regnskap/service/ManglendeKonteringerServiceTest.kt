package no.nav.bidrag.regnskap.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class ManglendeKonteringerServiceTest {

    @MockK
    private lateinit var oppdragsperiodeRepository: OppdragsperiodeRepository

    @MockK
    private lateinit var persistenceService: PersistenceService

    @InjectMockKs
    private lateinit var manglendeKonteringerService: ManglendeKonteringerService

    private val konteringerForeldetDato = "2006-04"

    @BeforeEach
    fun setup() {
        ReflectionTestUtils.setField(manglendeKonteringerService, "konteringerForeldetDato", konteringerForeldetDato)
    }

    @Test
    fun `skal opprette alle manglende konteringer for oppdragsperiode`() {
        val oppdrag = TestData.opprettOppdrag()
        val periodeTil = LocalDate.of(2010, 1, 1)
        val periodeFra = LocalDate.of(2006, 1, 1)
        val oppdragsperiode = TestData.opprettOppdragsperiode(
            oppdrag = oppdrag,
            periodeFra = periodeFra,
            periodeTil = periodeTil,
            konteringer = emptyList(),
        )
        oppdrag.oppdragsperioder = listOf(oppdragsperiode)
        val påløpsperiode = YearMonth.parse("2011-01")

        manglendeKonteringerService.opprettManglendeKonteringerForOppdragsperiode(oppdragsperiode, påløpsperiode, LocalDateTime.now())

        oppdragsperiode.konteringer shouldHaveSize 45
        oppdragsperiode.konteringer.first().overføringsperiode shouldBe konteringerForeldetDato
        oppdragsperiode.konteringer.last().overføringsperiode shouldBe YearMonth.from(periodeTil.minusMonths(1)).toString()
    }
}
