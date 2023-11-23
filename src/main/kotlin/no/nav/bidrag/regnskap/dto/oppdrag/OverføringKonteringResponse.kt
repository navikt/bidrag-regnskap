package no.nav.bidrag.regnskap.dto.oppdrag

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
    name = "OverføringKonteringResponse",
    description = "En overføring kontering er et forsøk, velykket eller ikke, på en overføring av en kontering.",
)
data class OverføringKonteringResponse(

    @field:Schema(
        description = "Id til overføringen.",
        example = "1",
    )
    val overføringId: Int,

    @field:Schema(
        description = "Id til konteringen overføringen tilhører.",
        example = "1",
    )
    val konteringId: Int?,

    @field:Schema(
        description = "Referansekode viser til batch-uid som er returnert fra ELIN.",
        example = "3f1248e9-8d19-4dc3-9584-84055421753d",
    )
    val referansekode: String?,

    @field:Schema(
        description = "Om vi har fått returnert en feilmelding i steden for en batch-uid fra Elin vil den ligge i dette feltet.",
        example = "-",
    )
    val feilmelding: String?,

    @field:Schema(
        description = "Tidspunkt for opprettelse av overføringen",
        format = "date-time",
        example = "2022-02-01:00:00:00",
    )
    val tidspunkt: LocalDateTime,

    @field:Schema(
        description = "Hvilken kanal denne overføringen ble utført i. Kan være Rest eller Påløpsfil.",
        example = "Rest",
    )
    val kanal: String,
)
