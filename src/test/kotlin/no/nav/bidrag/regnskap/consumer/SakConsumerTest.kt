package no.nav.bidrag.regnskap.consumer

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.regnskap.dto.sak.BidragSak
import no.nav.bidrag.regnskap.dto.sak.RolleDto
import no.nav.bidrag.regnskap.dto.sak.enumer.Bidragssakstatus
import no.nav.bidrag.regnskap.dto.sak.enumer.Rolletype
import no.nav.bidrag.regnskap.dto.sak.enumer.Sakskategori
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.ResponseEntity
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.client.RestTemplate

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
        every { restTemplate.getForEntity("$sakUrl$SAK_PATH/123", BidragSak::class.java) } returns ResponseEntity.ok(
            opprettBidragSak(Rolletype.BM)
        )

        val fødelsnummer = sakConsumer.hentBmFraSak("123")

        fødelsnummer shouldNotBe DUMMY_NUMMER
    }

    @Test
    fun `skal bruke dummynr om det ikke finnes en bm på sak`() {
        every { restTemplate.getForEntity("$sakUrl$SAK_PATH/123", BidragSak::class.java) } returns ResponseEntity.ok(
            opprettBidragSak(Rolletype.BP)
        )

        val fødelsnummer = sakConsumer.hentBmFraSak("123")

        fødelsnummer shouldBe DUMMY_NUMMER
    }

    private fun opprettBidragSak(rolletype: Rolletype): BidragSak {
        return BidragSak(
            "eierfogd",
            "123",
            Bidragssakstatus.NY,
            Sakskategori.N,
            roller = listOf(
                RolleDto(
                    PersonidentGenerator.genererFødselsnummer(),
                    rolletype
                )
            )
        )
    }
}
