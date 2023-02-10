package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.consumer.SakConsumer
import no.nav.bidrag.regnskap.dto.oppdrag.OppdragsperiodeResponse
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OppdragsperiodeService(
  val konteringService: KonteringService,
  val sakConsumer: SakConsumer
) {

  fun hentOppdragsperioderMedKonteringer(oppdrag: Oppdrag): List<OppdragsperiodeResponse> {
    return (oppdrag.oppdragsperioder)?.map {
        OppdragsperiodeResponse(
          oppdragsperiodeId = it.oppdragsperiodeId,
          oppdragId = it.oppdrag?.oppdragId,
          vedtakId = it.vedtakId,
          gjelderIdent = it.gjelderIdent,
          mottakerIdent = it.mottakerIdent,
          belop = it.beløp,
          valuta = it.valuta,
          periodeFra = it.periodeFra.toString(),
          periodeTil = it.periodeTil.toString(),
          vedtaksdato = it.vedtaksdato.toString(),
          opprettetAv = it.opprettetAv,
          delytelseId = it.delytelseId.toString(),
          aktivTil = it.aktivTil.toString(),
          konteringer = konteringService.hentKonteringer(oppdrag)
        )
    } ?: emptyList()
  }

  fun opprettNyeOppdragsperioder(
    hendelse: Hendelse, oppdrag: Oppdrag
  ): List<Oppdragsperiode> {
    return hendelse.periodeListe.filter { it.beløp != null }.map {
        Oppdragsperiode(
          vedtakId = hendelse.vedtakId,
          gjelderIdent = sakConsumer.hentBmFraSak(hendelse.sakId),
          mottakerIdent = hendelse.mottakerIdent,
          beløp = it.beløp!!,
          valuta = it.valutakode!!,
          periodeFra = it.periodeFomDato,
          periodeTil = it.periodeTilDato,
          vedtaksdato = hendelse.vedtakDato,
          opprettetAv = hendelse.opprettetAv,
          delytelseId = it.referanse,
          oppdrag = oppdrag
        )
    }
  }

  fun settAktivTilDatoPåEksisterendeOppdragsperioder(oppdrag: Oppdrag, periodeFra: LocalDate) {
    oppdrag.oppdragsperioder?.filter { it.aktivTil == null }?.forEach {
      if (it.periodeTil != null && periodeFra.isAfter(it.periodeTil)) {
        it.aktivTil = it.periodeTil
      } else if (periodeFra.isBefore(it.periodeFra)) {
        it.aktivTil = it.periodeFra
      } else {
        it.aktivTil = periodeFra
      }
    }
  }
}