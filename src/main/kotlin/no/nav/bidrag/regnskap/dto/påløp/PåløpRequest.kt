package no.nav.bidrag.regnskap.dto.påløp

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.time.YearMonth

@Schema(description = "Et påløp representerer en kjøring av overføring av påløpsfil til skatt.")
data class PåløpRequest(

    @field:Schema(
        description = "Dato paløpet skal kjøre.",
        format = "date-time",
        example = "2022-01-01T16:00:00Z",
        type = "String",
        required = true,
    )
    val kjoredato: LocalDateTime,

    @field:Schema(
        description = "Perioden påløpet gjelder for.",
        type = "String",
        format = "yyyy-MM",
        example = "2022-01",
        required = true,
    )
    val forPeriode: YearMonth,
)
