package no.nav.bidrag.regnskap.dto.krav

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Kravfeil", description = "Lister feil i et krav.")
data class Kravfeil(
  val kravKonteringsfeil: List<KravKonteringsfeil>
)