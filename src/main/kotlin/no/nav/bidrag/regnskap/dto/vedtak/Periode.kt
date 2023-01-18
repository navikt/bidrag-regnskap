package no.nav.bidrag.regnskap.dto.vedtak

import java.math.BigDecimal
import java.time.LocalDate

data class Periode(
  val beløp: BigDecimal?,
  val valutakode: String?,
  val periodeFomDato: LocalDate,
  val periodeTilDato: LocalDate?,
  val referanse: Int?
  )
