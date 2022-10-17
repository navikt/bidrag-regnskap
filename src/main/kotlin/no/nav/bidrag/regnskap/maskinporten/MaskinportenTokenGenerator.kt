package no.nav.bidrag.regnskap.maskinporten

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import no.nav.bidrag.regnskap.config.MaskinportenConfig
import java.util.*

class MaskinportenTokenGenerator(
  val maskinportenConfig: MaskinportenConfig
) {

  internal fun genererJwtToken(scopes: List<String>): String {
    return SignedJWT(opprettJwsHeader(), generateJWTClaimSet(scopes)).apply {
      sign(RSASSASigner(opprettRsaKey()))
    }.serialize()
  }

  private fun generateJWTClaimSet(scopes: List<String>): JWTClaimsSet {
    return JWTClaimsSet.Builder().apply {
      audience(maskinportenConfig.audience)
      issuer(maskinportenConfig.issuer)
      claim("scope", scopes.joinToString(" "))
      issueTime(Date(Date().time - 2000))
      expirationTime(Date(Date().time + (maskinportenConfig.validInSeconds * 1000) - 2000))
    }.build()
  }

  private fun opprettJwsHeader(): JWSHeader = JWSHeader.Builder(JWSAlgorithm.RS256).keyID(opprettRsaKey().keyID).build()

  private fun opprettRsaKey(): RSAKey = RSAKey.parse(maskinportenConfig.privateKey)
}