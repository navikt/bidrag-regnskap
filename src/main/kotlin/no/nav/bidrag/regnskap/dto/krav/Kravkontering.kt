package no.nav.bidrag.regnskap.dto.krav

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.YearMonthSerializer
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.regnskap.dto.enumer.Søknadstype
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.dto.enumer.Type
import org.springframework.format.annotation.DateTimeFormat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Schema(
  description = "\t\n" +
      "En kontering angir hvor mye som skal betales av skyldner til mottaker på vegne av kravhaver.\n" +
      "\n" +
      "Konteringen kan unikt identifiseres med kombinasjonen transaksjonskode, delytelsesId og periode. Det forutsettes at delytelsesid'n er unik også på tvers av fagsystemid'er.\n" +
      "\n" +
      "Personidenter for gjelderIdent, kravhaverIdent, mottakerIdent og skyldnerIdent angis med enten FNR eller DNR. (Håndtering av BNR og NPID er uavklart.) Aktoernummer kan benyttes i kravhaverIdent, mottakerIdent og skyldnerIdent. Aktoernummere er elleve siffer og starter med enten 8 eller 9.\n" +
      "\n" +
      "I testmiljøene må Tenor-identer støttes i stedet for FNR/DNR. Disse identene har 8 eller 9 i tredje siffer."
)
data class Kravkontering(

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
    required = true
  ) val transaksjonskode: Transaksjonskode,

  @field:Schema(
    description = "Angir om det er en ny transaksjon eller en endring.",
    example = "NY",
    required = true
  ) val type: Type,

  @field:Schema(
    description = "Angirtypen behandling som har ført til konteringen.\n"
        + "| Kode | Beskrivelse                                                                |\n"
        + "|------|----------------------------------------------------------------------------|\n"
        + "| IN   | Sendes første måned i et indeksreguleringsvedtak. Etter dette benyttes EN. |\n"
        + "| FABM | Benyttes for gebyr som gjelder BM.                                         |\n"
        + "| FABP | Benyttes for gebyr som gjelder BP.                                         |\n"
        + "| EN   | Alle andre typer endringer. Også førstegangsvedtak.                        |\n",
    example = "EN",
    required = true
  ) val soknadType: Søknadstype,

  @field:Schema(
    description = "Personident (FNR/DNR) til bidragsmottaker i bidragssaken. I saker der bidragsmottaker ikke er satt benyttes et dummynr 22222222226",
    example = "15878598161",
    required = true
  ) val gjelderIdent: String,

  @field:Schema(
    description = "Personident (FNR/DNR) eller aktoernummer (TSS-ident/samhandler) til kravhaver." +
        "\n\nKravhaver angis ikke for gebyr.",
    example = "14871298182",
    required = false
  ) val kravhaverIdent: String?,


  @field:Schema(
    description = "Personident (FNR/DNR) eller aktoernummer (TSS-ident/samhandler) til mottaker av kravet." +
        "\n\nFor gebyr settes mottakerIdent til NAVs aktoernummer 80000345435.",
    example = "15878598161",
    required = true
  ) val mottakerIdent: String,

  @field:Schema(
    description = "Personident (FNR/DNR) eller aktoernummer (TSS-ident/samhandler) til skyldner. For Bidrag er dette BP i saken." +
        "\n\nFor forskudd settes skyldnerIdent til NAVs aktoernummer 80000345435.",
    example = "28848596401",
    required = true
  ) val skyldnerIdent: String,

  @field:Schema(
    description = "Konteringens beløp. Positive beløp og 0 regnes som tillegg, negative beløp som fradrag.",
    example = "2000.0",
    required = true
  ) val belop: BigDecimal,

  @field:Schema(
    description = "Valutakoden for beløpet.", example = "NOK", required = true
  ) val valuta: String,

  @field:Schema(
    description = "Angir hvilken periode (måned og år) konteringen gjelder.",
    type = "String",
    format = "yyyy-MM",
    example = "2022-04",
    required = true
  ) @field:JsonSerialize(using = YearMonthSerializer::class) @field:JsonDeserialize(using = YearMonthDeserializer::class) @field:DateTimeFormat(
    pattern = "yyyy-MM"
  ) val periode: YearMonth,

  @field:DateTimeFormat(pattern = "yyyy-MM-dd") @field:Schema(
    description = "Datoen vedtaket er fattet", example = "2022-03-18", required = true
  ) val vedtaksdato: LocalDate,

  @field:DateTimeFormat(pattern = "yyyy-MM-dd") @field:Schema(
    description = "Datoen kravet/konteringen gjøres klart for overføring. " +
        "For direkteoverførte online-vedtak blir datoen sannsynligvis det samme som vedtaksdato. " +
        "For påløp blir datoen satt til dagen påløpet genereres.",
    example = "2022-03-18",
    required = true
  ) val kjoredato: LocalDate,

  @field:Schema(
    description = "NAVs brukerid for saksbehandler som har fattet vedtaket",
    example = "a123456",
    required = true
  ) val saksbehandlerId: String,

  @field:Schema(
    description = "NAVs brukerid for saksbehandler som har attestert vedtaket (sannsynligvis samme som saksbehandlerId over).",
    example = "a123456",
    required = true
  ) val attestantId: String,

  @field:Schema(
    description = "Felt hvor utlandsavdelingen legger inn referansenummer (ffu-ref). " +
        "Dette er et fritekstfelt som kan inneholde spesialtegn.",
    example = "VII W → 450 → 40 /11",
    required = false
  ) val eksternReferanse: String?,

  @field:Schema(
    description = "Bidragssakens saksnummer angitt som String.",
    example = "2201234",
    required = true
  ) val fagsystemId: String,

  @field:Schema(
    description = "Unik referanse til oppdragsperioden i vedtaket angitt som String. " +
        "I bidragssaken kan en oppdragsperiode strekke over flere måneder, og samme referanse blir da benyttet for alle månedene. " +
        "Samme referanse kan ikke benyttes to ganger for samme transaksjonskode i samme måned.",
    example = "123456789",
    required = true
  ) val delytelsesId: String
)