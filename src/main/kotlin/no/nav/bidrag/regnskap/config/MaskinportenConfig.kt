package no.nav.bidrag.regnskap.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("maskinporten")
data class MaskinportenConfig(
  val tokenUrl: String,
  val audience: String,
  val clientId: String,
  val scope: String,
  val privateKey: String,
  val validInSeconds: Int
)

