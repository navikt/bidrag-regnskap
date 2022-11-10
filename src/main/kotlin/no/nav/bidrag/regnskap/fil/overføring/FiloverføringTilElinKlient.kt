package no.nav.bidrag.regnskap.fil.overføring

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import no.nav.bidrag.regnskap.config.FiloverføringTilElinConfig
import no.nav.bidrag.regnskap.persistence.bucket.PåløpsfilBucket
import org.springframework.stereotype.Service

@Service
class FiloverføringTilElinKlient(
  private val config: FiloverføringTilElinConfig,
  private val påløpsfilBucket: PåløpsfilBucket
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
    var session: Session? = null
    val channel: ChannelSftp?
    try {
      session = jSch.getSession(config.username, config.host, config.port)
      session.setConfig("StrictHostKeyChecking", "no")
      session.connect(1000)

      channel = session.openChannel(FiloverføringTilElinConfig.JSCH_CHANNEL_TYPE_SFTP) as ChannelSftp
      channel.connect()
      channel.cd(config.directory)
      channel.put(påløpsfilBucket.hentFil(filmappe + filnavn), filnavn)
    } finally {
      session?.disconnect()
    }
  }
}