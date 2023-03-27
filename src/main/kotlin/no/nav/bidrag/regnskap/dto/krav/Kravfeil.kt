package no.nav.bidrag.regnskap.dto.krav

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Kravfeil", description = "Lister feil i et krav.")
data class Kravfeil(

    @field:Schema(
        description = "En beskrivelse av feilen som har oppstått. " +
                "Feilmeldingen er ment å være forståelig for et menneske ved manuell gjennomgang.",
        example = "Tolkning feilet i Elin."
    )
    val message: String
)