package no.nav.bidrag.regnskap.util

import no.nav.bidrag.domain.enums.EngangsbelopType
import no.nav.bidrag.domain.enums.VedtakType
import no.nav.bidrag.domain.enums.regnskap.Søknadstype
import no.nav.bidrag.domain.enums.regnskap.Type
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import java.time.YearMonth

object KonteringUtils {

    fun vurderSøknadType(hendelse: Hendelse, indexPeriode: Int): String {
        return if (hendelse.vedtakType == VedtakType.INDEKSREGULERING && indexPeriode == 0) {
            Søknadstype.IR.name
        } else if (hendelse.type == EngangsbelopType.GEBYR_MOTTAKER.name) {
            Søknadstype.FABM.name
        } else if (hendelse.type == EngangsbelopType.GEBYR_SKYLDNER.name) {
            Søknadstype.FABP.name
        } else {
            Søknadstype.EN.name
        }
    }

    fun vurderSøknadType(vedtakType: String, stønadType: String, indexPeriode: Int): String {
        return if (vedtakType == VedtakType.INDEKSREGULERING.name && indexPeriode == 0) {
            Søknadstype.IR.name
        } else if (stønadType == EngangsbelopType.GEBYR_MOTTAKER.name) {
            Søknadstype.FABM.name
        } else if (stønadType == EngangsbelopType.GEBYR_SKYLDNER.name) {
            Søknadstype.FABP.name
        } else {
            Søknadstype.EN.name
        }
    }

    fun vurderType(oppdragsperiode: Oppdragsperiode, periode: YearMonth): String {
        if (finnesKonteringForPeriode(oppdragsperiode, periode)) {
            return Type.NY.name
        }
        return Type.ENDRING.name
    }

    private fun finnesKonteringForPeriode(oppdragsperiode: Oppdragsperiode, periode: YearMonth) =
        oppdragsperiode.oppdrag?.oppdragsperioder?.none { it.konteringer.any { kontering -> kontering.overføringsperiode == periode.toString() } } == true
}
