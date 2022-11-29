package no.nav.bidrag.regnskap.dto.påløp

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.regnskap.dto.enumer.ÅrsakKode

@Schema(name = "Vedlikeholdsmodus", description = "Kontroll av vedlikeholdsmodus.")
data class Vedlikeholdsmodus(

  @field:Schema(
    description = "Angir om vedlikeholdsmodus skal skrus på eller ikke.",
    example = "true",
    defaultValue = "true",
    required = true
  )
  val aktiv: Boolean,

  @field:Schema(
    description = "Forhåndsavklart kode som beskriver årsaken til vedlikeholdsmodus.",
    example = "PAALOEP_GENERERES",
    required = true
  )
  val aarsakKode: ÅrsakKode,

  @field:Schema(
    description = "Kommentar som sier noe om hvorfor påløpsmodus er aktiv.",
    example = "Påløp for 2022-12 genereres hos NAV.",
    required = true
  )
  val kommentar: String,
)