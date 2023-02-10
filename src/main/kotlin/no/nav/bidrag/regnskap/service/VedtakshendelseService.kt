package no.nav.bidrag.regnskap.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.bidrag.behandling.felles.dto.vedtak.Engangsbelop
import no.nav.bidrag.behandling.felles.dto.vedtak.Stonadsendring
import no.nav.bidrag.behandling.felles.dto.vedtak.VedtakHendelse
import no.nav.bidrag.behandling.felles.enums.Innkreving
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

    LOGGER.debug("Behandler vedakHendelse for vedtakid: ${vedtakHendelse.id}")
    SECURE_LOGGER.debug("Behandler vedtakHendelse: $vedtakHendelse")

    vedtakHendelse.stonadsendringListe?.forEach {
      opprettOppdragForStonadsending(vedtakHendelse, it)
    }

    vedtakHendelse.engangsbelopListe?.forEach {
      opprettOppdragForEngangsbelop(vedtakHendelse, it)
    }

    LOGGER.debug("Ferdig med behandling av vedtakshendelse: ${vedtakHendelse.id}")
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

    if(erInnkrevingOgEndring(stonadsendring.innkreving, stonadsendring.endring)) {
      val hendelse = Hendelse(
        type = stonadsendring.type.name,
        vedtakType = vedtakHendelse.type,
        kravhaverIdent = leggTilIdent(stonadsendring.kravhaverId),
        skyldnerIdent = leggTilIdent(stonadsendring.skyldnerId),
        mottakerIdent = leggTilIdent(stonadsendring.mottakerId),
        sakId = stonadsendring.sakId,
        vedtakId = vedtakHendelse.id,
        vedtakDato = vedtakHendelse.vedtakTidspunkt.toLocalDate(),
        opprettetAv = vedtakHendelse.opprettetAv,
        eksternReferanse = vedtakHendelse.eksternReferanse,
        utsattTilDato = vedtakHendelse.utsattTilDato,
        periodeListe = mapPeriodelisteTilDomene(stonadsendring.periodeListe)
      )
      val oppdragId = oppdragService.lagreHendelse(hendelse)

      kravService.sendKrav(oppdragId)
    }
  }

  private fun erInnkrevingOgEndring(innkreving: Innkreving, endring: Boolean): Boolean {
    return innkreving == Innkreving.JA && endring
  }

  private fun mapPeriodelisteTilDomene(periodeListe: List<no.nav.bidrag.behandling.felles.dto.vedtak.Periode>): List<Periode> {
    val perioder = mutableListOf<Periode>()
    periodeListe.forEach { periode ->
      perioder.add(
        Periode(
          beløp = periode.belop,
          valutakode = periode.valutakode,
          periodeFomDato = periode.fomDato,
          periodeTilDato = periode.tilDato,
          referanse = periode.referanse?.let { Integer.valueOf(it) }
        )
      )
    }
    return perioder
  }

  private fun opprettOppdragForEngangsbelop(vedtakHendelse: VedtakHendelse, engangsbelop: Engangsbelop) {
    LOGGER.debug("Oppretter oppdrag for engangsbeløp.")

    if(erInnkrevingOgEndring(engangsbelop.innkreving, engangsbelop.endring)) {
      val hendelse = Hendelse(
        engangsbeløpId = engangsbelop.id,
        endretEngangsbeløpId = engangsbelop.endrerId,
        type = engangsbelop.type.name,
        vedtakType = vedtakHendelse.type,
        kravhaverIdent = leggTilIdent(engangsbelop.kravhaverId),
        skyldnerIdent = leggTilIdent(engangsbelop.skyldnerId),
        mottakerIdent = leggTilIdent(engangsbelop.mottakerId),
        sakId = engangsbelop.sakId,
        vedtakId = vedtakHendelse.id,
        vedtakDato = vedtakHendelse.vedtakTidspunkt.toLocalDate(),
        opprettetAv = vedtakHendelse.opprettetAv,
        eksternReferanse = vedtakHendelse.eksternReferanse,
        utsattTilDato = vedtakHendelse.utsattTilDato,
        periodeListe = listOf(
          Periode(
            periodeFomDato = vedtakHendelse.vedtakTidspunkt.toLocalDate().withDayOfMonth(1),
            periodeTilDato = vedtakHendelse.vedtakTidspunkt.toLocalDate().withDayOfMonth(1).plusMonths(1),
            beløp = engangsbelop.belop,
            valutakode = engangsbelop.valutakode,
            referanse = engangsbelop.referanse?.let { Integer.valueOf(it) }
          )
        )
      )
      val oppdragId = oppdragService.lagreHendelse(hendelse)

      kravService.sendKrav(oppdragId)
    }
  }

  private fun leggTilIdent(ident: String): String {
    return if (ident == "NAV") NAV_TSS_IDENT else ident
  }
}