package no.nav.bidrag.regnskap.maskinporten

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class MaskinportenConfig(

  @Value("\${maskinporten.tokenUrl}")
  val tokenUrl: String,

  @Value("\${maskinporten.audience}")
  val audience: String,

  @Value("\${maskinporten.clientId}")
  val clientId: String,

  @Value("\${maskinporten.scope}")
  val scope: String,

  @Value("\${maskinporten.privateKey}")
  val privateKey: String,

  @Value("\${maskinporten.validInSeconds}")
  val validInSeconds: Int
)

