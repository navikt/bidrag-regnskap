package no.nav.bidrag.regnskap.model

import java.time.YearMonth

data class KravResponse(
  val konteringsfeil: List<Konteringsfeil>
)

data class Konteringsfeil(
    val feilkode: String,
    val feilmelding: String,
    val konteringId: KonteringId
)

data class KonteringId(
  val transaksjonskode: Transaksjonskode,
  val periode: YearMonth,
  val delytelsesId: String
)
