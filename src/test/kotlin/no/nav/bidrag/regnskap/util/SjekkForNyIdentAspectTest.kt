package no.nav.bidrag.regnskap.util

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.commons.util.SjekkForNyIdent
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.regnskap.BidragRegnskapLocal
import no.nav.bidrag.regnskap.consumer.PersonApiWireMock
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("h2")
@DirtiesContext
@EnableMockOAuth2Server
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [BidragRegnskapLocal::class])
class SjekkForNyIdentAspectTest {

    private val ident1: String = PersonidentGenerator.genererFødselsnummer()
    private val ident2 = PersonidentGenerator.genererFødselsnummer()

    @Autowired
    private lateinit var dummyClassForAnnotasjon: DummyClassForAnnotasjon

    private var personApiWireMock: PersonApiWireMock = PersonApiWireMock()

    @BeforeAll
    fun setup() {
        personApiWireMock.personidentMedGyldigResponse()
    }

    @Test
    fun `skal bytte begge identer`() {
        val nyeIdenter = dummyClassForAnnotasjon.skalTesteBytteAvBeggeIdenter(ident1, ident2)

        nyeIdenter[0] shouldBe personApiWireMock.nyIdent
        nyeIdenter[1] shouldBe personApiWireMock.nyIdent
    }

    @Test
    fun `skal bytte siste identer`() {
        val nyeIdenter = dummyClassForAnnotasjon.skalTesteBytteAvSisteIdent(ident1, ident2)

        nyeIdenter[0] shouldNotBe personApiWireMock.nyIdent
        nyeIdenter[1] shouldBe personApiWireMock.nyIdent
    }

    @Test
    fun `skal bytte midterste identer`() {
        val nyeIdenter = dummyClassForAnnotasjon.skalTesteBytteAvMidtersteIdent(ident1, ident2, 123)

        nyeIdenter[0] shouldNotBe personApiWireMock.nyIdent
        nyeIdenter[1] shouldBe personApiWireMock.nyIdent
        nyeIdenter[2] shouldNotBe personApiWireMock.nyIdent
    }

    @Test
    fun `skal bytte ident på parameter`() {
        val nyeIdenter = dummyClassForAnnotasjon.skalTesteBytteAvIdentPåParameter(ident1, ident2)

        nyeIdenter[0] shouldBe personApiWireMock.nyIdent
        nyeIdenter[1] shouldNotBe personApiWireMock.nyIdent
    }

    @Test
    fun `skal bytte ident på begge parameter`() {
        val nyeIdenter = dummyClassForAnnotasjon.skalTesteBytteAvIdentPåBeggeParameter(ident1, ident2)

        nyeIdenter[0] shouldBe personApiWireMock.nyIdent
        nyeIdenter[1] shouldBe personApiWireMock.nyIdent
    }

    @Test
    fun `skal bytte ident med Ident som objekt`() {
        val nyeIdenter = dummyClassForAnnotasjon.skalTesteBytteAvIdentMedIdentObjekt(Ident(ident1), ident2)

        nyeIdenter[0] shouldBe personApiWireMock.nyIdent
        nyeIdenter[1] shouldNotBe personApiWireMock.nyIdent
    }

    @Test
    fun `skal bytte ident på parameter med Ident som objekt`() {
        val nyeIdenter = dummyClassForAnnotasjon.skalTesteBytteAvIdentPåParameterMedIdentObjekt(Ident(ident1), ident2)

        nyeIdenter[0] shouldBe personApiWireMock.nyIdent
        nyeIdenter[1] shouldNotBe personApiWireMock.nyIdent
    }

    @Test
    fun `skal ha feil input objekt`() {
        val nyeIdenter = dummyClassForAnnotasjon.skalTesteFeilInput(ident1.toLong(), ident2)

        nyeIdenter[0] shouldNotBe personApiWireMock.nyIdent
        nyeIdenter[1] shouldNotBe personApiWireMock.nyIdent
    }

    @Test
    fun `skal ha feil input objekt på parameter`() {
        val nyeIdenter = dummyClassForAnnotasjon.skalTesteFeilInputPåParameter(ident1.toLong(), ident2)

        nyeIdenter[0] shouldNotBe personApiWireMock.nyIdent
        nyeIdenter[1] shouldNotBe personApiWireMock.nyIdent
    }
}

@Component
private class DummyClassForAnnotasjon {
    @SjekkForNyIdent("ident1", "ident2")
    fun skalTesteBytteAvBeggeIdenter(ident1: String, ident2: String): List<String> {
        return listOf(ident1, ident2)
    }

    @SjekkForNyIdent("ident2")
    fun skalTesteBytteAvSisteIdent(ident1: String, ident2: String): List<String> {
        return listOf(ident1, ident2)
    }

    @SjekkForNyIdent("ident2")
    fun skalTesteBytteAvMidtersteIdent(ident1: String, ident2: String, noeAnnet: Int): List<String> {
        return listOf(ident1, ident2, "$noeAnnet")
    }

    fun skalTesteBytteAvIdentPåParameter(@SjekkForNyIdent ident1: String, ident2: String): List<String> {
        return listOf(ident1, ident2)
    }

    fun skalTesteBytteAvIdentPåBeggeParameter(@SjekkForNyIdent ident1: String, @SjekkForNyIdent ident2: String): List<String> {
        return listOf(ident1, ident2)
    }

    @SjekkForNyIdent("ident1")
    fun skalTesteBytteAvIdentMedIdentObjekt(ident1: Ident, ident2: String): List<String> {
        return listOf(ident1.verdi, ident2)
    }

    fun skalTesteBytteAvIdentPåParameterMedIdentObjekt(@SjekkForNyIdent ident1: Ident, ident2: String): List<String> {
        return listOf(ident1.verdi, ident2)
    }

    fun skalTesteFeilInputPåParameter(@SjekkForNyIdent ident1: Long, ident2: String): List<String> {
        return listOf(ident1.toString(), ident2)
    }

    @SjekkForNyIdent("ident1")
    fun skalTesteFeilInput(ident1: Long, ident2: String): List<String> {
        return listOf(ident1.toString(), ident2)
    }
}
