package no.nav.bidrag.regnskap.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.bidrag.behandling.felles.dto.vedtak.Engangsbelop
import no.nav.bidrag.behandling.felles.dto.vedtak.Periode
import no.nav.bidrag.behandling.felles.dto.vedtak.Stonadsendring
import no.nav.bidrag.behandling.felles.dto.vedtak.VedtakHendelse
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.hendelse.vedtak.Hendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(VedtakHendelseService::class.java)
private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())

@Service
class VedtakHendelseService(
  private val oppdragService: OppdragService
) {

  fun behandleHendelse(hendelse: String) {
    val vedtakHendelse = mapVedtakHendelse(hendelse)

    LOGGER.debug("Behandler vedakHendelse for vedtakid: ${vedtakHendelse.vedtakId}")
    SECURE_LOGGER.debug("Behandler vedtakHendelse: $vedtakHendelse")

    vedtakHendelse.stonadsendringListe?.forEach {
      opprettOppdragForStonadsending(vedtakHendelse, it)
    }

    vedtakHendelse.engangsbelopListe?.forEach {
      opprettOppdragForEngangsbelop(vedtakHendelse, it)
    }

    LOGGER.debug("Ferdig med behandling av vedtakshendelse: ${vedtakHendelse.vedtakId}")
  }

  fun mapVedtakHendelse(hendelse: String): VedtakHendelse {
    return try {
      objectMapper.readValue(hendelse, VedtakHendelse::class.java)
    } finally {
      SECURE_LOGGER.debug("Leser hendelse: {}", hendelse)
    }
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
      endretEngangsbelopId = engangsbelop.endrerEngangsbelopId,
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