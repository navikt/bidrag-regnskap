package no.nav.bidrag.regnskap.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties("maskinporten")
data class MaskinportenConfig
@ConstructorBinding
constructor(
    val tokenUrl: String,
    val audience: String,
    val clientId: String,
    val scope: String,
    val privateKey: String,
    val validInSeconds: Int
)
