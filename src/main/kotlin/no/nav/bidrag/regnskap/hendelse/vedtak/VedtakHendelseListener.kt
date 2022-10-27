package no.nav.bidrag.regnskap.hendelse.vedtak

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.behandling.felles.dto.vedtak.Engangsbelop
import no.nav.bidrag.behandling.felles.dto.vedtak.Periode
import no.nav.bidrag.behandling.felles.dto.vedtak.Stonadsendring
import no.nav.bidrag.behandling.felles.dto.vedtak.VedtakHendelse
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.service.OppdragService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener

private val LOGGER = LoggerFactory.getLogger(VedtakHendelseListener::class.java)

class VedtakHendelseListener(
  private val oppdragService: OppdragService, private val objectMapper: ObjectMapper
) {

  @KafkaListener(
    groupId = "bidrag-regnskap", topics = ["\${TOPIC_VEDTAK}"], errorHandler = "vedtakshendelseErrorHandler"
  )
  fun lesHendelse(hendelse: String) {
    try {
      val vedtakHendelse = mapVedtakHendelse(hendelse)

      behandleHendelse(vedtakHendelse)
    } catch (e: JacksonException) {
      LOGGER.error("Mapping av hendelse feilet! Se secure log for mer informasjon.")
      SECURE_LOGGER.error("Mapping av hendelse feilet! Hendelse: $hendelse Feil: $e")
    }
  }

  private fun mapVedtakHendelse(hendelse: String): VedtakHendelse {
    return try {
      objectMapper.readValue(hendelse, VedtakHendelse::class.java)
    } finally {
      SECURE_LOGGER.debug("Leser hendelse: {}", hendelse)
    }
  }

  fun behandleHendelse(vedtakHendelse: VedtakHendelse) {
    LOGGER.info("Behandler vedakHendelse for vedtakid: ${vedtakHendelse.vedtakId}")
    SECURE_LOGGER.info("Behandler vedtakHendelse: $vedtakHendelse")

    vedtakHendelse.stonadsendringListe?.forEach {
      opprettOppdragForStonadsending(vedtakHendelse, it)
    }

    vedtakHendelse.engangsbelopListe?.forEach {
      opprettOppdragForEngangsbelop(vedtakHendelse, it)
    }

    LOGGER.info("Ferdig med behandling av vedtakshendelse: ${vedtakHendelse.vedtakId}")
  }

  private fun opprettOppdragForStonadsending(vedtakHendelse: VedtakHendelse, stonadsendring: Stonadsendring) {
    LOGGER.debug("Oppretter oppdrag for stonadendring.")

    val hendelse = Hendelse(
      type = stonadsendring.stonadType.name,
      vedtakType = vedtakHendelse.vedtakType,
      kravhaverIdent = stonadsendring.kravhaverId,
      skyldnerIdent = stonadsendring.skyldnerId,
      mottakerIdent = stonadsendring.mottakerId,
      sakId = stonadsendring.sakId,
      vedtakId = vedtakHendelse.vedtakId,
      vedtakDato = vedtakHendelse.vedtakDato,
      opprettetAv = vedtakHendelse.opprettetAv,
      eksternReferanse = vedtakHendelse.eksternReferanse,
      utsattTilDato = vedtakHendelse.utsattTilDato,
      periodeListe = stonadsendring.periodeListe
    )
    oppdragService.lagreHendelse(hendelse)
  }

  private fun opprettOppdragForEngangsbelop(vedtakHendelse: VedtakHendelse, engangsbelop: Engangsbelop) {
    LOGGER.debug("Oppretter oppdrag for engangsbeløp.")
    val hendelse = Hendelse(
      engangsbelopId = engangsbelop.engangsbelopId,
      type = engangsbelop.type.name,
      vedtakType = vedtakHendelse.vedtakType,
      kravhaverIdent = engangsbelop.kravhaverId,
      skyldnerIdent = engangsbelop.skyldnerId,
      mottakerIdent = engangsbelop.mottakerId,
      sakId = engangsbelop.sakId,
      vedtakId = vedtakHendelse.vedtakId,
      vedtakDato = vedtakHendelse.vedtakDato,
      opprettetAv = vedtakHendelse.opprettetAv,
      eksternReferanse = vedtakHendelse.eksternReferanse,
      utsattTilDato = vedtakHendelse.utsattTilDato,
      periodeListe = listOf(
        Periode(
          periodeFomDato = vedtakHendelse.vedtakDato.withDayOfMonth(1),
          periodeTilDato = vedtakHendelse.vedtakDato.withDayOfMonth(1).plusMonths(1),
          belop = engangsbelop.belop,
          valutakode = engangsbelop.valutakode,
          resultatkode = engangsbelop.resultatkode,
          referanse = engangsbelop.referanse
        )
      )
    )
    oppdragService.lagreHendelse(hendelse)
  }
}