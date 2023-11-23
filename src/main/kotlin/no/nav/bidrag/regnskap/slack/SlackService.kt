package no.nav.bidrag.regnskap.slack

import com.slack.api.Slack
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SlackService(
    @Value("\${BIDRAG_BOT_SLACK_OAUTH_TOKEN}") private val oauthToken: String,
) {

    companion object {
        const val CHANNEL = "C0556GXJPMF"
        private val LOGGER = KotlinLogging.logger { }
    }

    fun sendMelding(melding: String, threadTs: String? = null): SlackMelding {
        val response = Slack.getInstance().methods(oauthToken).chatPostMessage {
            it.channel(CHANNEL)
                .threadTs(threadTs)
                .text(melding)
        }

        if (response.isOk) {
            LOGGER.debug { "Slack melding sendt: $melding" }
        } else {
            LOGGER.error { "Feil ved sending av slackmelding: ${response.error}" }
        }
        return SlackMelding(ts = response.ts, channel = response.channel)
    }

    inner class SlackMelding(
        private val ts: String?,
        private val threadTs: String? = ts,
        private val channel: String?,
    ) {

        fun oppdaterMelding(melding: String) {
            if (ts == null) {
                LOGGER.warn { "Ingen melding å oppdatere..." }
                return
            }
            val response = Slack.getInstance().methods(oauthToken).chatUpdate {
                it.channel(channel)
                    .ts(ts)
                    .text(melding)
            }
            if (response.isOk) {
                LOGGER.trace { "Slack melding oppdatert: $melding" }
                return
            } else {
                LOGGER.error { "Feil ved oppdatering av slackmelding: ${response.error}" }
            }
        }

        fun svarITråd(melding: String): SlackMelding {
            if (ts == null) {
                LOGGER.trace { "Ingen melding å svare på..." }
                return this
            }
            return sendMelding(melding = melding, threadTs = threadTs)
        }
    }
}
