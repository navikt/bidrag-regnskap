package no.nav.bidrag.regnskap.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.behandling.felles.dto.vedtak.VedtakHendelse
import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.regnskap.SECURE_LOGGER
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(BehandleHendelseService::class.java)

@Service
class BehandleHendelseService(private val objectMapper: ObjectMapper){

  fun mapVedtakHendelse(hendelse: String): VedtakHendelse {
    return try {
      objectMapper.readValue(hendelse, VedtakHendelse::class.java)
    } finally {
      SECURE_LOGGER.debug("Leser hendelse: {}", hendelse)
    }
  }

  fun behandleHendelse(vedtakHendelse: VedtakHendelse) {
    LOGGER.info("Behandler vedakHendelse for vedtakid: ${vedtakHendelse.vedtakId}")
    SECURE_LOGGER.info("Behandler vedtakHendelse: $vedtakHendelse")

    when (vedtakHendelse.hentStonadType()) {
      StonadType.BIDRAG, StonadType.FORSKUDD -> LOGGER.warn("IKKE_IMPLEMENTERT ${vedtakHendelse.stonadType}") //TODO: IMPLEMENTERE
      StonadType.NO_SUPPORT -> LOGGER.warn("bidrag-regnskap støtter ikke hendelsen ${vedtakHendelse.stonadType}")
      else -> {
        LOGGER.warn("Bidrag-regnskap ukjent stønadtype ${vedtakHendelse.stonadType}")
      }
    }
  }
}