package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.consumer.SakConsumer
import no.nav.bidrag.regnskap.dto.oppdrag.OppdragsperiodeResponse
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.dto.vedtak.Periode
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
    return (oppdrag.oppdragsperioder).map {
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
        referanse = it.eksternReferanse,
        aktivTil = it.aktivTil.toString(),
        konteringer = konteringService.hentKonteringer(oppdrag)
      )
    }
  }

  fun opprettNyOppdragsperiode(hendelse: Hendelse, periode: Periode, oppdrag: Oppdrag): Oppdragsperiode {
    return Oppdragsperiode(
      vedtakId = hendelse.vedtakId,
      vedtakType = hendelse.vedtakType.toString(),
      gjelderIdent = sakConsumer.hentBmFraSak(hendelse.sakId),
      mottakerIdent = hendelse.mottakerIdent,
      beløp = periode.beløp!!,
      valuta = periode.valutakode!!,
      periodeFra = periode.periodeFomDato,
      periodeTil = periode.periodeTilDato,
      vedtaksdato = hendelse.vedtakDato,
      opprettetAv = hendelse.opprettetAv,
      delytelseId = periode.referanse,
      eksternReferanse = hendelse.eksternReferanse,
      oppdrag = oppdrag
    )
  }

  fun opprettNyeOppdragsperioder(
    hendelse: Hendelse, oppdrag: Oppdrag
  ): List<Oppdragsperiode> {
    return hendelse.periodeListe.map {
      Oppdragsperiode(
        vedtakId = hendelse.vedtakId,
        vedtakType = hendelse.vedtakType.toString(),
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

  fun settAktivTilDatoPåEksisterendeOppdragsperioder(oppdrag: Oppdrag, nyOppdragsperiodePeriodeFra: LocalDate) {
    oppdrag.oppdragsperioder.forEach {
      if (it.aktivTil != null && it.aktivTil!!.isBefore(nyOppdragsperiodePeriodeFra)) {
        return@forEach
      } else if (it.periodeTil != null && nyOppdragsperiodePeriodeFra.isAfter(it.periodeTil)) {
        it.aktivTil = it.periodeTil
      } else {
        it.aktivTil = nyOppdragsperiodePeriodeFra
      }
    }
  }
}