package no.nav.bidrag.regnskap.dto.påløp

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Et påløp representerer en kjøring av overføring av påløpsfil til skatt.")
data class PåløpResponse(

    @field:Schema(description = "Id for påløpet.")
    val påløpId: Int,

    @field:Schema(
        description = "Dato paløpet skal kjøre.",
        example = "2022-01-01",
    )
    val kjoredato: String,

    @field:Schema(
        description = "Tidspunkt påløpet ble startet.",
        type = "datetime",
        example = "2022-01-01 00:00:00",
    )
    var startetTidspunkt: String? = null,

    @field:Schema(
        description = "Tidspunkt påløpet ble fullført.",
        type = "datetime",
        example = "2022-01-01 00:00:00",
    )
    var fullfortTidspunkt: String? = null,

    @field:Schema(description = "Perioden påløpet gjelder for.")
    val forPeriode: String,
)
