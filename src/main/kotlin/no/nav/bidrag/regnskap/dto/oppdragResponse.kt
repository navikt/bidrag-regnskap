package no.nav.bidrag.regnskap.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(
  name = "OppdragResponse",
  description = "Et oppdrag består av en liste med oppdragsperioder som inneholder konteringer."
)
data class OppdragResponse(

  @field:Schema(
    description = "Id til oppdraget.",
    example = "10"
  )
  val oppdragId: Int?,

  @field:Schema(
    description = "Type stønad.",
    example = "BIDRAG"
  )
  val type: String,

  @field:Schema(
    description = "Personident (FNR/DNR) eller aktoernummer (TSS-ident/samhandler) til kravhaver." +
        "\n\nKravhaver angis ikke for gebyr.",
    example = "12345678910"
  )
  val kravhaverIdent: String?,

  @field:Schema(
    description = "Personident (FNR/DNR) eller aktoernummer (TSS-ident/samhandler) til skyldner. " +
        "For Bidrag er dette BP i saken." +
        "\n\nFor forskudd settes skyldnerIdent til NAVs aktoernummer 80000345435.",
    example = "12345678910"
  )
  val skyldnerIdent: String,

  @field:Schema(
    description = "Referanse til gebyr.",
    example = "ABC123"
  )
  val referanse: String?,

  @field:Schema(
    description = "Sist overførte periode for kontering knyttet til oppdraget.",
    example = "2022-01"
  )
  val sistOversendtePeriode: String?,

  @field:Schema(
    description = "Sist endret tidspunkt for oppdraget.",
    format = "date-time",
    example = "2022-02-01:00:00:00",
  )
  val endretTidspunkt: String?,

  @field:Schema(
    description = "Referanse til IDen til engangsbeløp. Vil ikke eksistere for stønader.",
    example = "123",
  )
  val engangsbelopId: Int?,

  @field:Schema(
    description = "Liste over alle oppdragsperioder til oppdraget."
  )
  val oppdragsperioder: List<OppdragsperiodeResponse>
)

@Schema(
  name = "OppdragsperiodeResponse",
  description = "En oppdragsperiode er en oversikt over en betaling over en gitt tidsperiode. " +
      "Kun en oppdragsperiode kan være aktiv på en hver tid. Resterende oppdragsperioder er historiske. "
      + "Oppdragsperioden inneholder alle konteringer sendt til skatt for denne tidsperioden."
)
data class OppdragsperiodeResponse(

  @field:Schema(
    description = "Id til oppdragsperioden.",
    example = "20"
  )
  val oppdragsperiodeId: Int?,

  @field:Schema(
    description = "Id til oppdraget oppdragsperioden tilhører.",
    example = "10"
  )
  val oppdragId: Int?,

  @field:Schema(
    description = "SakId for bidragssaken.",
    example = "123456"
  )
  val sakId: String,

  @field:Schema(
    description = "VedtaksId for vedtaket oppdraget gjelder for.",
    example = "123456"
  )
  val vedtakId: Int,

  @field:Schema(
    description = "Personident (FNR/DNR) til bidragsmottaker i bidragssaken. " +
        "I saker der bidragsmottaker ikke er satt benyttes et dummynr 22222222226",
    example = "12345678910"
  )
  val gjelderIdent: String,

  @field:Schema(
    description = "Personident (FNR/DNR) eller aktoernummer (TSS-ident/samhandler) til mottaker av kravet." +
        "\n\nFor gebyr settes mottakerIdent til NAVs aktoernummer 80000345435.",
    example = "12345678910"
  )
  val mottakerIdent: String,

  @field:Schema(
    description = "Beløpet oppdraget er på.",
    example = "7500"
  )
  val belop: BigDecimal,

  @field:Schema(
    description = "Valutaen beløpet er i.",
    example = "NOK"
  )
  val valuta: String,

  @field:Schema(
    description = "Datoen utbetalingen skal starte fra.",
    format = "date",
    example = "2022-01-01"
  )
  val periodeFra: String,

  @field:Schema(
    description = "Datoen utbetalingen skal opphøre.",
    format = "date",
    example = "2022-02-01"
  )
  val periodeTil: String,

  @field:Schema(
    description = "Datoen vedtaket ble fattet.",
    format = "date",
    example = "2022-01-01"
  )
  val vedtaksdato: String,

  @field:Schema(
    description = "SaksbehandlerId til saksbehandler som fattet vedtaket.",
    example = "123456789"
  )
  val opprettetAv: String,

  @field:Schema(
    description = "Unik referanse til oppdragsperioden i vedtaket angitt som String. " +
        "I bidragssaken kan en oppdragsperiode strekke over flere måneder, og samme referanse blir da benyttet for alle månedene. " +
        "Samme referanse kan ikke benyttes to ganger for samme transaksjonskode i samme måned.",
    example = "qwerty123456"
  )
  val delytelseId: String,

  @field:Schema(
    description = "Felt for å se om oppdragsperioden er aktiv og da hvilken dato den er aktiv til.",
    format = "date",
    example = "2022-01-01"
  )
  val aktivTil: String?,

  @field:Schema(
    description = "Liste over alle konteringer som tilhører oppdragsperioden."
  )
  val konteringer: List<KonteringResponse>
)

@Schema(
  name = "KonteringResponse",
  description = "En kontering er et overført oppdrag til skatt for en måned i en oppdragsperiode."
)
data class KonteringResponse(

  @field:Schema(
    description = "Id til konteringen.",
    example = "30"
  )
  val konteringId: Int?,

  @field:Schema(
    description = "Id til oppdragsperioden konteringen tilhører.",
    example = "20"
  )
  val oppdragsperiodeId: Int?,

  @field:Schema(
    description = "Type transaksjon.\n\n"
        + "Gyldige transaksjonskoder er:\n"
        + "| Kode  | Korreksjonskode | Beskrivelse                                |\n"
        + "|-------|-----------------|--------------------------------------------|\n"
        + "| A1    | A3              | Bidragsforskudd                            |\n"
        + "| B1    | B3              | Underholdsbidrag (m/u tilleggsbidrag)      |\n"
        + "| D1    | D3              | 18årsbidrag                                |\n"
        + "| E1    | E3              | Bidrag til særlige utgifter (særtilskudd)  |\n"
        + "| F1    | F3              | Ekrefellebidrag                            |\n"
        + "| G1    | G3              | Gebyr                                      |\n"
        + "| H1    | H3              | Tilbakekreving                             |\n"
        + "| I1    |                 | Motregning                                 |\n"
        + "| K1    |                 | Ettergivelse                               |\n"
        + "| K2    |                 | Direkte oppgjør (innbetalt beløp)          |\n"
        + "| K3    |                 | Tilbakekreving ettergivelse                |\n",
    example = "B1",
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
    example = "2022-02-01:00:00:00",
  )
  val overforingstidspunkt: String?,

  @field:Schema(
    description = "Angir om det er en ny transaksjon eller en endring.",
    example = "NY"
  )
  val type: Type?,

  @field:Schema(
    description = "Dersom konteringen representerer et justert beløp settes dette feltet. " +
        "Justeringstypene er INDEKSREGULERING og ALDERSJUSTERING. " +
        "Dersom konteringen ikke gjelder en av de automatiske justeringstypene blir ikke feltet benyttet. "
        + "For blant annet Jackson deserialisering i Java gir dette en NULL-verdi for feltet. " +
        "Feltet settes kun for første måned med justert beløp.",
    example = "INDEKSREGULERING",
  )
  val justering: Justering?,

  @field:Schema(
    description = "Dersom konteringen gjelder gebyr må feltet settes for å angi om det gjelder gebyr for bidragsmottaker eller bidragspliktig. " +
        "Dersom konteringen ikke gjelder gebyr (G1 eller G3) blir ikke feltet gebyrRolle benyttet.",
    example = "BIDRAGSMOTTAKER"
  )
  val gebyrRolle: String?,

  @field:Schema(
    description = "Angir om konteringen har blitt overført i generert påløpsfil eller om den er sendt over via REST-endepunktet.",
    example = "false"
  )
  val sendtIPalopsfil: Boolean?
)

