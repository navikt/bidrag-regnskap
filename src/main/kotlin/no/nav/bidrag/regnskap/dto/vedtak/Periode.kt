package no.nav.bidrag.regnskap.dto.vedtak

import java.math.BigDecimal
import java.time.LocalDate

data class Periode(
    val beløp: BigDecimal?,
    val valutakode: String?,
    var periodeFomDato: LocalDate,
    var periodeTilDato: LocalDate?,
    val delytelsesId: Int?,
)
