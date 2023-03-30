package no.nav.bidrag.regnskap.maskinporten

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Date
import kotlin.math.absoluteValue

class MaskinportenTokenGeneratorTest {
    private val scopes = listOf("skatt:testscope.read", "skatt:testscope.write")

    @Test
    fun `Skal sjekke at maskonporten token er signed med privat key i config`() {
        val config = MaskinportenWireMock.createMaskinportenConfig()
        val generator = MaskinportenTokenGenerator(config)
        val signedJWT = SignedJWT.parse(generator.genererJwtToken(scopes))
        val verifier: JWSVerifier = RSASSAVerifier(RSAKey.parse(config.privateKey).toRSAPublicKey())

        signedJWT.verify(verifier) shouldBe true
    }

    @Test
    fun `Skal sjekke at benyttet algorytme i header er rsa256`() {
        val config = MaskinportenWireMock.createMaskinportenConfig()
        val generator = MaskinportenTokenGenerator(config)
        val signedJWT = SignedJWT.parse(generator.genererJwtToken(scopes))

        (signedJWT.header.algorithm as JWSAlgorithm).name shouldBe "RS256"
    }

    @Test
    fun `Skal sjekke at scope claims er lagt til i token body`() {
        val config = MaskinportenWireMock.createMaskinportenConfig()
        val generator = MaskinportenTokenGenerator(config)
        val signedJWT = SignedJWT.parse(generator.genererJwtToken(scopes))

        signedJWT.jwtClaimsSet.audience[0] shouldBe config.audience
        signedJWT.jwtClaimsSet.issuer shouldBe config.clientId
        signedJWT.jwtClaimsSet.getStringClaim("scope").split(' ') shouldBe scopes
    }

    @Test
    fun `Skal sjekke at timestamps blir satt riktig på token body`() {
        val config = MaskinportenWireMock.createMaskinportenConfig()
        val generator = MaskinportenTokenGenerator(config)
        val signedJWT = SignedJWT.parse(generator.genererJwtToken(scopes))

        val issuedAt = signedJWT.jwtClaimsSet.issueTime
        val expirationTime = signedJWT.jwtClaimsSet.expirationTime

        Date() likInnenEtSekund issuedAt shouldBe true
        Date() plusSekunder config.validInSeconds likInnenEtSekund expirationTime shouldBe true
    }

    private infix fun Date.likInnenEtSekund(date: Date): Boolean = (time - date.time).absoluteValue < 1000L
    private infix fun Date.plusSekunder(seconds: Int): Date = Date(time + seconds * 1000)
}
