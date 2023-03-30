package no.nav.bidrag.regnskap.maskinporten

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MaskinportenClientTest {
    private var maskinportenWireMock = MaskinportenWireMock()
    private lateinit var maskinportenClient: MaskinportenClient

    private val config = MaskinportenWireMock.createMaskinportenConfig()

    @BeforeEach
    internal fun setup() {
        maskinportenWireMock.reset()
        maskinportenClient = MaskinportenClient(config)
    }

    @AfterAll
    internal fun teardown() {
        maskinportenWireMock.stop()
    }

    @Test
    fun `Skal gjennbruke token fra maskinporten om det ikke har utgått`() {
        maskinportenWireMock.medGyldigResponseForKunEtKall()

        val forsteToken = maskinportenClient.hentMaskinportenToken(config.scope)
        val andreToken = maskinportenClient.hentMaskinportenToken(config.scope)

        forsteToken shouldBeSameInstanceAs andreToken
    }

    @Test
    fun `Skal kaste MaskinportenException hvis innholdet i token ikke kan mappes`() {
        maskinportenWireMock.medUgyldigResponse()

        val exception = shouldThrow<MaskinportenClientException> { maskinportenClient.hentMaskinportenToken(config.scope) }

        exception.message shouldStartWith "Feil ved deserialisering av response fra maskinporten"
    }

    @Test
    fun `Skal kaste MaskinportenException hvis maskinporten returnerer internal server error`() {
        maskinportenWireMock.med500Error()

        val exception = shouldThrow<MaskinportenClientException> { maskinportenClient.hentMaskinportenToken(config.scope) }

        exception.message shouldStartWith "Feil ved henting av token: Status:"
    }
}
