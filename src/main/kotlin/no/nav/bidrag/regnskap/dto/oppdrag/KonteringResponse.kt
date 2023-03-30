package no.nav.bidrag.regnskap.dto.oppdrag

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.regnskap.dto.enumer.Søknadstype
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.dto.enumer.Type
import java.time.LocalDateTime

@Schema(
    name = "KonteringResponse",
    description = "En kontering er et overført oppdrag til skatt for en måned i en oppdragsperiode."
)
data class KonteringResponse(

    @field:Schema(
        description = "Id til konteringen.",
        example = "30"
    )
    val konteringId: Int,

    @field:Schema(
        description = "Id til oppdragsperioden konteringen tilhører.",
        example = "20"
    )
    val oppdragsperiodeId: Int?,

    @field:Schema(
        description = "Type transaksjon.\n\n" +
            "Gyldige transaksjonskoder er:\n" +
            "| Kode  | Korreksjonskode | Beskrivelse                                |\n" +
            "|-------|-----------------|--------------------------------------------|\n" +
            "| A1    | A3              | Bidragsforskudd                            |\n" +
            "| B1    | B3              | Underholdsbidrag (m/u tilleggsbidrag)      |\n" +
            "| D1    | D3              | 18årsbidrag                                |\n" +
            "| E1    | E3              | Bidrag til særlige utgifter (særtilskudd)  |\n" +
            "| F1    | F3              | Ekrefellebidrag                            |\n" +
            "| G1    | G3              | Gebyr                                      |\n" +
            "| H1    | H3              | Tilbakekreving                             |\n" +
            "| I1    |                 | Motregning                                 |\n" +
            "| K1    |                 | Ettergivelse                               |\n" +
            "| K2    |                 | Direkte oppgjør (innbetalt beløp)          |\n" +
            "| K3    |                 | Tilbakekreving ettergivelse                |\n",
        example = "B1"
    )
    val transaksjonskode: Transaksjonskode,

    @field:Schema(
        description = "Angir hvilken periode (måned og år) konteringen gjelder.",
        type = "String",
        example = "2022-04",
        required = true
    )
    val overforingsperiode: String,

    @field:Schema(
        description = "Tidspunktet overføringen ble gjennomført. ",
        format = "date-time",
        example = "2022-02-01:00:00:00"
    )
    val overforingstidspunkt: LocalDateTime?,

    @field:Schema(
        description = "Angir om det er en ny transaksjon eller en endring.",
        example = "NY"
    )
    val type: Type?,

    @field:Schema(
        description = "Angirtypen behandling som har ført til konteringen.\n" +
            "| Kode | Beskrivelse                                                                |\n" +
            "|------|----------------------------------------------------------------------------|\n" +
            "| IN   | Sendes første måned i et indeksreguleringsvedtak. Etter dette benyttes EN. |\n" +
            "| FABM | Benyttes for gebyr som gjelder BM.                                         |\n" +
            "| FABP | Benyttes for gebyr som gjelder BP.                                         |\n" +
            "| EN   | Alle andre typer endringer. Også førstegangsvedtak.                        |\n",
        example = "EN",
        required = true
    ) val soknadType: Søknadstype,

    @field:Schema(
        description = "Angir om konteringen har blitt overført i generert påløpsfil eller om den er sendt over via REST-endepunktet.",
        example = "false"
    )
    val sendtIPalopsfil: Boolean
)
