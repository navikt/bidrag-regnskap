package no.nav.bidrag.regnskap.dto.oppdrag

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(
    name = "OppdragsperiodeResponse",
    description = "En oppdragsperiode er en oversikt over en betaling over en gitt tidsperiode. " +
        "Kun en oppdragsperiode kan være aktiv på en hver tid. Resterende oppdragsperioder er historiske. " +
        "Oppdragsperioden inneholder alle konteringer sendt til skatt for denne tidsperioden.",
)
data class OppdragsperiodeResponse(

    @field:Schema(
        description = "Id til oppdragsperioden.",
        example = "20",
    )
    val oppdragsperiodeId: Int,

    @field:Schema(
        description = "Id til oppdraget oppdragsperioden tilhører.",
        example = "10",
    )
    val oppdragId: Int?,

    @field:Schema(
        description = "VedtaksId for vedtaket oppdraget gjelder for.",
        example = "123456",
    )
    val vedtakId: Int,

    @field:Schema(
        description = "Referanse for vedtaket oppdraget gjelder for.",
        example = "SøknadsidVedtakA-Rolleid",
    )
    val referanse: String?,

    @field:Schema(
        description = "Beløpet oppdraget er på.",
        example = "7500",
    )
    val belop: BigDecimal,

    @field:Schema(
        description = "Valutaen beløpet er i.",
        example = "NOK",
    )
    val valuta: String,

    @field:Schema(
        description = "Datoen utbetalingen skal opphøre.",
        format = "date",
        example = "2022-02-01",
    )
    val periodeTil: String,

    @field:Schema(
        description = "Datoen utbetalingen skal starte fra.",
        format = "date",
        example = "2022-01-01",
    )
    val periodeFra: String,

    @field:Schema(
        description = "Datoen vedtaket ble fattet.",
        format = "date",
        example = "2022-01-01",
    )
    val vedtaksdato: String,

    @field:Schema(
        description = "SaksbehandlerId til saksbehandler som fattet vedtaket.",
        example = "123456789",
    )
    val opprettetAv: String,

    @field:Schema(
        description = "Sier om oppdragsperioden er opphørende og ikke skal fortsettes.",
        example = "false",
    )
    val opphørendeOppdragsperiode: Boolean,

    @field:Schema(
        description = "Unik referanse til oppdragsperioden i vedtaket angitt som String. " +
            "I bidragssaken kan en oppdragsperiode strekke over flere måneder, og samme referanse blir da benyttet for alle månedene. " +
            "Samme referanse kan ikke benyttes to ganger for samme transaksjonskode i samme måned.",
        example = "10000001",
    )
    val delytelseId: Int?,

    @field:Schema(
        description = "Ekstern referanse til gebyr.",
        example = "ABC123",
    )
    val eksternReferanse: String?,

    @field:Schema(
        description = "Felt for å se om oppdragsperioden er aktiv og da hvilken dato den er aktiv til.",
        format = "date",
        example = "2022-01-01",
    )
    val aktivTil: String?,

    @field:Schema(
        description = "Liste over alle konteringer som tilhører oppdragsperioden.",
    )
    val konteringer: List<KonteringResponse>,
)
