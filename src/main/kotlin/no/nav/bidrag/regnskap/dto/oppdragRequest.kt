package no.nav.bidrag.regnskap.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.behandling.felles.enums.StonadType
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

@Schema(
  name = "OppdragRequest",
  description = "Et oppdrag består av en liste med oppdragsperioder som inneholder konteringer."
)
data class OppdragRequest(

  @field:Schema(
    description = "Type stønad.",
    example = "BIDRAG",
    required = true
  )
  val stonadType: StonadType,

  @field:Schema(
    description = "Personident (FNR/DNR) eller aktoernummer (TSS-ident/samhandler) til kravhaver." +
        "\n\nKravhaver angis ikke for gebyr.",
    example = "12345678910",
    required = true
  )
  val kravhaverIdent: String,

  @field:Schema(
    description = "Personident (FNR/DNR) eller aktoernummer (TSS-ident/samhandler) til skyldner. " +
        "For Bidrag er dette BP i saken." +
        "\n\nFor forskudd settes skyldnerIdent til NAVs aktoernummer 80000345435.",    example = "12345678910",
    required = true)
  val skyldnerIdent: String,

  @field:Schema(
    description = "SaksId for bidragssaken.",
    example = "123456",
    required = true)
  val sakId: Int,

  @field:Schema(
    description = "Referanse til gebyr.",
    example = "ABC123",
    required = false)
  val referanse: String? = null,

  @field:Schema(
    description = "VedtaksId for vedtaket oppdraget gjelder for.",
    example = "123456",
    required = true)
  val vedtakId: Int,

  @field:Schema(
    description = "Personident (FNR/DNR) til bidragsmottaker i bidragssaken. " +
        "I saker der bidragsmottaker ikke er satt benyttes et dummynr 22222222226",
    example = "12345678910",
    required = true
  )
  val gjelderIdent: String,

  @field:Schema(
    description = "Personident (FNR/DNR) eller aktoernummer (TSS-ident/samhandler) til mottaker av kravet." +
        "\n\nFor gebyr settes mottakerIdent til NAVs aktoernummer 80000345435.",
    example = "12345678910",
    required = true
  )
  val mottakerIdent: String,

  @field:Schema(
    description = "Beløpet oppdraget er på.",
    example = "7500",
    required = true
  )
  val belop: Int,

  @field:Schema(
    description = "Valutaen beløpet er i.",
    example = "NOK",
    required = true)
  val valuta: String,

  @field:Schema(
    description = "Datoen utbetalingen skal starte fra.",
    format = "date",
    example = "2022-01-01",
    required = true
  )
  @field:DateTimeFormat(pattern = "yyyy-MM-dd")
  val periodeFra: LocalDate,

  @field:Schema(
    description = "Datoen utbetalingen skal opphøre.",
    format = "date",
    example = "2022-02-01",
    required = true
  )
  @field:DateTimeFormat(pattern = "yyyy-MM-dd")
  val periodeTil: LocalDate,

  @field:Schema(
    description = "Datoen vedtaket ble fattet.",
    format = "date",
    example = "2022-01-01",
    required = true
  )
  @field:DateTimeFormat(pattern = "yyyy-MM-dd")
  val vedtaksdato: LocalDate,

  @field:Schema(
    description = "SaksbehandlerId til saksbehandler som fattet vedtaket.",
    example = "123456789",
    required = true
  )
  val opprettetAv: String,

  @field:Schema(
    description = "Unik referanse til oppdragsperioden i vedtaket angitt som String. " +
        "I bidragssaken kan en oppdragsperiode strekke over flere måneder, og samme referanse blir da benyttet for alle månedene. " +
        "Samme referanse kan ikke benyttes to ganger for samme transaksjonskode i samme måned.",
    example = "qwerty123456")
  val delytelseId: String?,

  @field:Schema(
    description = "Om ønskelig kan saksbehandler velge å utsette sending av faktura for et vedtak. " +
        "I de tilfellene settes denne datoen til en dato frem i tid.",
    format = "date",
    example = "2022-02-09",
    required = false
  )
  @field:DateTimeFormat(pattern = "yyyy-MM-dd")
  val utsattTilDato: LocalDate? = null,

  @field:Schema(
    description = "Fritekstfelt. Benyttes av utlandsavdelingen.",
    example = "VII W → 450 → 40 /11",
    required = false
  )
  val tekst: String? = null,
)
