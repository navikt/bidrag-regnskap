package no.nav.bidrag.regnskap.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.util.Base64

@ConstructorBinding
@ConfigurationProperties("sftp")
class FiloverføringTilElinConfig(
    val username: String,
    val host: String,
    val port: Int,
    var privateKey: String,
    val directory: String = "inbound",
    val skalOverforeFil: Boolean
) {

    companion object {
        const val JSCH_CHANNEL_TYPE_SFTP = "sftp"
    }

    val privateKeyDecoded = Base64.getDecoder().decode(privateKey)
}
