package no.nav.bidrag.regnskap.fil.overføring

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import no.nav.bidrag.regnskap.config.FiloverføringTilElinConfig
import no.nav.bidrag.regnskap.persistence.bucket.GcpFilBucket
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(FiloverføringTilElinKlient::class.java)

@Service
class FiloverføringTilElinKlient(
    private val config: FiloverføringTilElinConfig,
    private val gcpFilBucket: GcpFilBucket
) {

    private val jSch = JSch().apply {
        addIdentity(
            config.username,
            config.privateKeyDecoded,
            null,
            null
        )
    }

    fun lastOppFilTilFilsluse(filmappe: String, filnavn: String) {
        LOGGER.info("Start oppkobling mot filsluse...")
        var session: Session? = null
        val channel: ChannelSftp?
        try {
            session = jSch.getSession(config.username, config.host, config.port)
            session.setConfig("StrictHostKeyChecking", "no")
            session.connect(15000)

            channel = session.openChannel(FiloverføringTilElinConfig.JSCH_CHANNEL_TYPE_SFTP) as ChannelSftp
            channel.connect()
            LOGGER.info("Oppkobling mot filsluse var vellykket!")
            LOGGER.info("Starter opplasting av fil: $filnavn fra GCP-bucket...")
            channel.cd(config.directory)
            channel.put(gcpFilBucket.hentFil(filmappe + filnavn), filnavn)
            LOGGER.info("Fil: $filnavn har blitt lastet opp på filsluse!")
        } finally {
            session?.disconnect()
        }
    }
}
