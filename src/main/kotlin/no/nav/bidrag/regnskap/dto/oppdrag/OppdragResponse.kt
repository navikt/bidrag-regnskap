package no.nav.bidrag.regnskap.dto.oppdrag

import io.swagger.v3.oas.annotations.media.Schema

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
