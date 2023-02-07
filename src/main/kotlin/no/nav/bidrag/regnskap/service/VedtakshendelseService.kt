package no.nav.bidrag.regnskap.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.bidrag.behandling.felles.dto.vedtak.Engangsbelop
import no.nav.bidrag.behandling.felles.dto.vedtak.Stonadsendring
import no.nav.bidrag.behandling.felles.dto.vedtak.VedtakHendelse
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.dto.vedtak.Periode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(VedtakshendelseService::class.java)
private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())

@Service
class VedtakshendelseService(
  private val oppdragService: OppdragService,
  private val kravService: KravService
) {

  companion object {
    const val NAV_TSS_IDENT = "80000345435"
  }

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
      kravhaverIdent = leggTilIdent(stonadsendring.kravhaverId),
      skyldnerIdent = leggTilIdent(stonadsendring.skyldnerId),
      mottakerIdent = leggTilIdent(stonadsendring.mottakerId),
      sakId = stonadsendring.sakId,
      vedtakId = vedtakHendelse.vedtakId,
      vedtakDato = vedtakHendelse.vedtakDato,
      opprettetAv = vedtakHendelse.opprettetAv,
      eksternReferanse = vedtakHendelse.eksternReferanse,
      utsattTilDato = vedtakHendelse.utsattTilDato,
      periodeListe = mapPeriodelisteTilDomene(stonadsendring.periodeListe)
    )
    val oppdragId = oppdragService.lagreHendelse(hendelse)

    kravService.sendKrav(oppdragId)
  }

  private fun opprettOppdragForEngangsbelop(vedtakHendelse: VedtakHendelse, engangsbelop: Engangsbelop) {
    LOGGER.debug("Oppretter oppdrag for engangsbeløp.")
    val hendelse = Hendelse(
      engangsbeløpId = engangsbelop.engangsbelopId,
      endretEngangsbeløpId = engangsbelop.endrerEngangsbelopId,
      type = engangsbelop.type.name,
      vedtakType = vedtakHendelse.vedtakType,
      kravhaverIdent = leggTilIdent(engangsbelop.kravhaverId),
      skyldnerIdent = leggTilIdent(engangsbelop.skyldnerId),
      mottakerIdent = leggTilIdent(engangsbelop.mottakerId),
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
          beløp = engangsbelop.belop,
          valutakode = engangsbelop.valutakode,
          referanse = engangsbelop.referanse?.let { Integer.valueOf(it) }
        )
      )
    )
    val oppdragId = oppdragService.lagreHendelse(hendelse)

    kravService.sendKrav(oppdragId)
  }

  private fun leggTilIdent(ident: String): String {
    return if (ident == "NAV") NAV_TSS_IDENT else ident
  }

  private fun mapPeriodelisteTilDomene(periodeListe: List<no.nav.bidrag.behandling.felles.dto.vedtak.Periode>): List<Periode> {
    val perioder = mutableListOf<Periode>()
    periodeListe.forEach { periode ->
      perioder.add(
        Periode(
          beløp = periode.belop,
          valutakode = periode.valutakode,
          periodeFomDato = periode.periodeFomDato,
          periodeTilDato = periode.periodeTilDato,
          referanse = periode.referanse?.let { Integer.valueOf(it) }
        )
      )
    }
    return perioder
  }
}