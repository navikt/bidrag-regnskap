package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.dto.vedtak.Periode
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class OppdragsperiodeService {

    fun opprettNyOppdragsperiode(hendelse: Hendelse, periode: Periode, oppdrag: Oppdrag): Oppdragsperiode {
        return Oppdragsperiode(
            vedtakId = hendelse.vedtakId,
            referanse = hendelse.referanse,
            vedtakType = hendelse.vedtakType.toString(),
            beløp = periode.beløp ?: BigDecimal.ZERO,
            valuta = periode.valutakode ?: "NOK",
            periodeFra = periode.periodeFomDato,
            periodeTil = periode.periodeTilDato,
            vedtaksdato = hendelse.vedtakDato,
            opprettetAv = hendelse.opprettetAv,
            enhetsnummer = hendelse.enhetsnummer?.verdi,
            delytelseId = periode.delytelsesId,
            eksternReferanse = hendelse.eksternReferanse,
            oppdrag = oppdrag,
            opphørendeOppdragsperiode = periode.beløp == null,
        )
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
