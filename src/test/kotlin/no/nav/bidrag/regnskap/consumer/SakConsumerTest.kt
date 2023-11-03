package no.nav.bidrag.regnskap.consumer

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.domene.bool.BegrensetTilgang
import no.nav.bidrag.domene.bool.LevdeAdskilt
import no.nav.bidrag.domene.bool.Paragraf19
import no.nav.bidrag.domene.bool.UkjentPart
import no.nav.bidrag.domene.enums.Bidragssakstatus
import no.nav.bidrag.domene.enums.Rolletype
import no.nav.bidrag.domene.enums.Sakskategori
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.streng.Enhetsnummer
import no.nav.bidrag.domene.streng.Saksnummer
import no.nav.bidrag.domene.tid.OpprettetDato
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.RolleDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.ResponseEntity
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class SakConsumerTest {

    @MockK
    private lateinit var restTemplate: RestTemplate

    @InjectMockKs
    private lateinit var sakConsumer: SakConsumer

    private val sakUrl = "localhost:8080"
    private val SAK_PATH = "/bidrag-sak/sak"
    private val DUMMY_NUMMER = "22222222226"

    @BeforeEach
    fun setup() {
        ReflectionTestUtils.setField(sakConsumer, "sakUrl", sakUrl)
    }

    @Test
    fun `skal hente ut fødelsnummer fra bm`() {
        every { restTemplate.getForEntity("$sakUrl$SAK_PATH/123", BidragssakDto::class.java) } returns ResponseEntity.ok(
            opprettBidragSak(Rolletype.BIDRAGSMOTTAKER)
        )

        val fødelsnummer = sakConsumer.hentBmFraSak("123")

        fødelsnummer shouldNotBe DUMMY_NUMMER
    }

    @Test
    fun `skal bruke dummynr om det ikke finnes en bm på sak`() {
        every { restTemplate.getForEntity("$sakUrl$SAK_PATH/123", BidragssakDto::class.java) } returns ResponseEntity.ok(
            opprettBidragSak(Rolletype.BIDRAGSPLIKTIG)
        )

        val fødelsnummer = sakConsumer.hentBmFraSak("123")

        fødelsnummer shouldBe DUMMY_NUMMER
    }

    private fun opprettBidragSak(rolletype: Rolletype): BidragssakDto {
        return BidragssakDto(
            Enhetsnummer("eierfogd"),
            Saksnummer("123"),
            Bidragssakstatus.NY,
            Sakskategori.N,
            Paragraf19(false),
            BegrensetTilgang(false),
            OpprettetDato(LocalDate.now()),
            LevdeAdskilt(false),
            UkjentPart(false),
            roller = listOf(
                RolleDto(
                    Personident(PersonidentGenerator.genererFødselsnummer()),
                    rolletype
                )
            )
        )
    }
}
