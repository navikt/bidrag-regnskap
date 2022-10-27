package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.OppdragsperiodeResponse
import no.nav.bidrag.regnskap.hendelse.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.util.*

@Service
class OppdragsperiodeService(
  val konteringService: KonteringService
) {

  fun hentOppdragsperioderMedKonteringer(oppdrag: Oppdrag): List<OppdragsperiodeResponse> {
    val oppdragsperiodeResponser = mutableListOf<OppdragsperiodeResponse>()

    (oppdrag.oppdragsperioder)?.forEach { oppdragsperiode ->
      oppdragsperiodeResponser.add(
        OppdragsperiodeResponse(
          oppdragsperiodeId = oppdragsperiode.oppdragsperiodeId,
          oppdragId = oppdragsperiode.oppdrag?.oppdragId,
          sakId = oppdragsperiode.sakId,
          vedtakId = oppdragsperiode.vedtakId,
          gjelderIdent = oppdragsperiode.gjelderIdent,
          mottakerIdent = oppdragsperiode.mottakerIdent,
          belop = oppdragsperiode.belop,
          valuta = oppdragsperiode.valuta,
          periodeFra = oppdragsperiode.periodeFra.toString(),
          periodeTil = oppdragsperiode.periodeTil.toString(),
          vedtaksdato = oppdragsperiode.vedtaksdato.toString(),
          opprettetAv = oppdragsperiode.opprettetAv,
          delytelseId = oppdragsperiode.delytelseId,
          aktivTil = oppdragsperiode.aktivTil.toString(),
          konteringer = konteringService.hentKonteringer(oppdrag)
        )
      )
    }
    return oppdragsperiodeResponser
  }

  fun opprettNyeOppdragsperioder(
    hendelse: Hendelse, oppdrag: Oppdrag
  ): List<Oppdragsperiode> {
    val oppdragsperiodeListe = mutableListOf<Oppdragsperiode>()

    hendelse.periodeListe.forEachIndexed { index, periode ->
      oppdragsperiodeListe.add(
        Oppdragsperiode(
          vedtakId = hendelse.vedtakId,
          sakId = hendelse.sakId,
          gjelderIdent = "22222222226", //TODO() Avklare med skatt. Dummynr per nå
          mottakerIdent = hendelse.mottakerIdent,
          belop = periode.belop.setScale(0, RoundingMode.HALF_UP).intValueExact(),
          valuta = periode.valutakode,
          periodeFra = periode.periodeFomDato,
          periodeTil = periode.periodeTilDato,
          vedtaksdato = hendelse.vedtakDato,
          opprettetAv = hendelse.opprettetAv,
          delytelseId = periode.referanse ?: genererRandomUUID(),
          aktivTil = periode.periodeTilDato,
          oppdrag = oppdrag
        )
      )
    }

    return oppdragsperiodeListe
  }

  private fun genererRandomUUID(): String {
    return UUID.randomUUID().toString()
  }
}