package no.nav.bidrag.regnskap.util

import no.nav.bidrag.behandling.felles.enums.EngangsbelopType
import no.nav.bidrag.behandling.felles.enums.VedtakType
import no.nav.bidrag.regnskap.dto.enumer.Søknadstype
import no.nav.bidrag.regnskap.dto.enumer.Type
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode

object KonteringUtils {

    fun vurderSøknadsType(hendelse: Hendelse, indexPeriode: Int): String {
        return if (hendelse.vedtakType == VedtakType.INDEKSREGULERING && indexPeriode == 0) {
            Søknadstype.IN.name
        } else if (hendelse.type == EngangsbelopType.GEBYR_MOTTAKER.name) {
            Søknadstype.FABM.name
        } else if (hendelse.type == EngangsbelopType.GEBYR_SKYLDNER.name) {
            Søknadstype.FABP.name
        } else {
            Søknadstype.EN.name
        }
    }

    fun vurderSøknadsType(vedtakType: String, indexPeriode: Int): String {
        return if (vedtakType == VedtakType.INDEKSREGULERING.name && indexPeriode == 0) {
            Søknadstype.IN.name
        } else if (vedtakType == EngangsbelopType.GEBYR_MOTTAKER.name) {
            Søknadstype.FABM.name
        } else if (vedtakType == EngangsbelopType.GEBYR_SKYLDNER.name) {
            Søknadstype.FABP.name
        } else {
            Søknadstype.EN.name
        }
    }

    fun vurderType(oppdragsperiode: Oppdragsperiode): String {
        if (oppdragsperiode.oppdrag?.oppdragsperioder?.none { it.konteringer.isNotEmpty() } == true) {
            return Type.NY.name
        }
        return Type.ENDRING.name
    }
}
