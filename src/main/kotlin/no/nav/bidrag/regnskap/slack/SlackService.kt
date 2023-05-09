package no.nav.bidrag.regnskap.slack

import com.slack.api.Slack
import io.github.oshai.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class SlackService(
    @Value("\${BIDRAG_BOT_SLACK_OAUTH_TOKEN}") private val oauthToken: String,
    @Value("\${NAIS_CLIENT_ID}") private val clientId: String
) {

    companion object {
        const val CHANNEL = "#team-bidrag-regnskap-varsel"
        private val LOGGER = KotlinLogging.logger { }
    }

    fun sendMelding(melding: String) {
        val response = Slack.getInstance().methods(oauthToken).chatPostMessage {
            it.channel(CHANNEL)
                .text("$melding\n\nOpphav for meldingen: $clientId")
        }

        if (response.isOk) {
            LOGGER.debug("Slack melding sendt: $melding")
        } else {
            LOGGER.error("Feil ved sending av slackmelding: ${response.error}")
        }
    }
}
