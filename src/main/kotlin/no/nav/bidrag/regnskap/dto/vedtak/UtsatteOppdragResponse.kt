package no.nav.bidrag.regnskap.dto.vedtak

import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.sak.Saksnummer
import java.time.LocalDate

data class UtsatteOppdragResponse(
    val utsatteOppdrag: List<UtsatteOppdrag>,
)

data class UtsatteOppdrag(
    val oppdragsid: Int,
    val saksnummer: Saksnummer,
    val stønadstype: Stønadstype? = null,
    val engangsbeløptype: Engangsbeløptype? = null,
    val utsattTilDato: LocalDate,
    val vedtak: List<Vedtak>,
)

data class Vedtak(
    val vedtaksid: Int,
    val vedtaksdato: LocalDate,
    val enhetsnummer: String?,
)
