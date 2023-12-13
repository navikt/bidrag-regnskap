package no.nav.bidrag.regnskap.util

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.regnskap.Type
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class KonteringUtilsTest {

    @Nested
    inner class VurderType {

        val oppdrag = TestData.opprettOppdrag(oppdragsperioder = emptyList())
        val oppdragsperiode = TestData.opprettOppdragsperiode(
            oppdrag = oppdrag,
            periodeFra = LocalDate.now().minusMonths(3),
            periodeTil = LocalDate.now(),
        )

        @BeforeEach
        fun setup() {
            oppdrag.oppdragsperioder = oppdrag.oppdragsperioder.plus(oppdragsperiode)
        }

        @Test
        fun `Skal vurdere til type NY dersom det ikke finnes noen kontering for den perioden`() {
            val type = KonteringUtils.vurderType(oppdragsperiode.oppdrag?.oppdragsperioder ?: emptyList(), YearMonth.now().plusMonths(1))
            type shouldBe Type.NY.name
        }

        @Test
        fun `Skal vurdere til type NY dersom det ikke finnes noen kontering for den perioden og period er tilbake i tid`() {
            val type = KonteringUtils.vurderType(oppdragsperiode.oppdrag?.oppdragsperioder ?: emptyList(), YearMonth.now().minusMonths(6))
            type shouldBe Type.NY.name
        }

        @Test
        fun `Skal vurdere til type NY dersom det ikke finnes noen kontering for den perioden og period er langt frem i tid`() {
            val type = KonteringUtils.vurderType(oppdragsperiode.oppdrag?.oppdragsperioder ?: emptyList(), YearMonth.now().plusMonths(18))
            type shouldBe Type.NY.name
        }

        @Test
        fun `Skal vurdere til type ENDRING dersom det finnes kontering for perioden`() {
            val type = KonteringUtils.vurderType(oppdragsperiode.oppdrag?.oppdragsperioder ?: emptyList(), YearMonth.now())
            type shouldBe Type.ENDRING.name
        }
    }
}
