package no.nav.bidrag.regnskap.dto.vedlikeholdsmodus

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Feilsituasjon", description = "Et kall har ført til feilmelding")
data class Feilsituasjon(

  @field:Schema(
    description = "En kode som beskriver feilsituasjonen. Koden skal være lik for like situasjoner.",
    example = "UGYLDIG_TOKEN",
    required = true)
  val feilkode: String,
  @field:Schema(
    description = "En tekst som beskriver denne konkrete feilsituasjonen. Beskrivelsen bør inneholde nok informasjon til at problemet kan løses.",
    example = "Token utløp 2022-11-01 10:00:00",
    required = true)
  val feilmelding: String
)
