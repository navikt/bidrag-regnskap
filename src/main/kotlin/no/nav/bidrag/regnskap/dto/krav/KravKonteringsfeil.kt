package no.nav.bidrag.regnskap.dto.krav

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Konteringsfeil", description = "Beskriver feil i en enkelt kontering.")
data class KravKonteringsfeil(

  @field:Schema(
    description = "En kode som angir type feil som har oppstått. " +
        "Feilkoden er ment å kunne brukes til å maskinelt sortere feil.",
    example = "TOLKNING"
  )
  val feilkode: String,

  @field:Schema(
    description = "En beskrivelse av feilen som har oppstått. " +
        "Feilmeldingen er ment å være forståelig for et menneske ved manuell gjennomgang.",
    example = "Tolkning feilet i Elin."
  )
  val feilmelding: String,

  @field:Schema(description = "Identifiserer hvilken kontering som førte til feilen.")
  val kravKonteringId: KravKonteringId
)