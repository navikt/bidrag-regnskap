package no.nav.bidrag.regnskap.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.YearMonthSerializer
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate
import java.time.YearMonth

const val KRAV_BESKRIVELSE =
  "Operasjon for å levere krav fra NAV til regnskapet hos Skatteetaten. " +
      "Et krav består av en liste med konteringer. Det forventes at disse konteringen behandles samlet. " +
      "Det vil si at hvis én av konteringene feiler, skal ingen av konteringene i kravet benyttes.\n" + "\n" +
      "Dersom et krav feiler kan det forsøkes overført på nytt gjentatte ganger inntil kravet er overført. " +
      "Krav som gjelder samme fagsak må leveres i korrekt rekkefølge. " +
      "Feiler et krav i en sak, skal ikke senere krav i samme sak overføres. " +
      "Senere krav i andre saker kan overføres, selv om noen av partene fra den feilende fagsaken er involvert.\n" + "\n" +
      "Det forventes at et krav alltid inneholder de samme konteringene. " +
      "Dersom et nytt vedtak fører til et nytt krav som venter på et tidligere feilende krav, skal ikke konteringene fra det seneste kravet slås sammen med det ventende kravet.\n" + "\n" +
      "NAV har ansvar for å manuelt følge opp krav som ved flere forsøk ikke kan overføres, og vil løse opp i problemet i samarbeid med Skatteetaten.\n" + "\n" +
      "Ved månedlig påløp skal ikke dette grensesnittet benyttes. " + "Tilsvarende krav legges i stedet inn i en fil som overføres til Skatteetaten gjennom filslusa.\n" + "\n" +
      "Formatet på påløpsfilen skal være tilsvarende det nye grensesnittet, men hvor hvert krav legges inn på egen linje."

@Schema(name = "SkattKonteringRequest", description = "Et krav består av en liste med konteringer.")
data class SkattKonteringerRequest(
  val konteringer: List<SkattKontering>
)

@Schema(
  description = "En kontering angir hvor mye som skal betales av skyldner til mottaker på vegne av kravhaver.\n" +
      "\nKonteringen kan unikt identifiseres med kombinasjonen transaksjonskode, delytelsesId og periode. " +
      "Det forutsettes at delytelsesid'n er unik også på tvers av fagsystemid'er.\n" +
      "\nPersonidenter for gjelderIdent, kravhaverIdent, mottakerIdent og skyldnerIdent angis med enten FNR eller DNR. " +
      "(Håndtering av BNR og NPID er uavklart.) Aktoernummer kan benyttes i kravhaverIdent, mottakerIdent og skyldnerIdent. " +
      "Aktoernummere er elleve siffer og starter med enten 8 eller 9.\n" +
      "\nI testmiljøene må Tenor-identer støttes i stedet for FNR/DNR. Disse identene har 8 eller 9 i tredje siffer."
)
data class SkattKontering(

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
    description = "Dersom konteringen representerer et justert beløp settes dette feltet. " +
        "Justeringstypene er INDEKSREGULERING og ALDERSJUSTERING. " +
        "Dersom konteringen ikke gjelder en av de automatiske justeringstypene blir ikke feltet benyttet. "
        + "For blant annet Jackson deserialisering i Java gir dette en NULL-verdi for feltet. " +
        "Feltet settes kun for første måned med justert beløp.",
    example = "INDEKSREGULERING",
    required = false
  ) val justering: Justering?,

  @field:Schema(
    description = "Dersom konteringen gjelder gebyr må feltet settes for å angi om det gjelder gebyr for bidragsmottaker eller bidragspliktig. " +
        "Dersom konteringen ikke gjelder gebyr (G1 eller G3) blir ikke feltet gebyrRolle benyttet.",
    example = "",
    nullable = true,
    required = false
  ) val gebyrRolle: GebyrRolle?,

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
    example = "2000",
    required = true
  ) val belop: Int,

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
        "Dette er et fritekstfelt som kan inneholde spesialtegn." +
        "\n\nTODO: Bedre navn på feltet? Hva blir riktig regnskapsmessig?",
    example = "VII W → 450 → 40 /11",
    required = false
  ) val tekst: String?,

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

enum class Transaksjonskode(val beskrivelse: String, val korreksjonskode: String?) {
  A1("Bidragsforskudd", "A3"),
  A3("Bidragsforskudd", null),
  B1("Underholdsbidrag (m/u tilleggsbidrag)", "B3"),
  B3("Underholdsbidrag (m/u tilleggsbidrag)", null),
  D1("18årsbidrag", "D3"),
  D3("18årsbidrag", null),
  E1("Bidrag til særlige utgifter (særtilskudd)", "E3"),
  E3("Bidrag til særlige utgifter (særtilskudd)", null),
  F1("Ektefellebidrag", "F3"),
  F3("Ektefellebidrag", null),
  G1("Gebyr", "G3"),
  G3("Gebyr", null),
  H1("Tilbakekreving", "H3"),
  H3("Tilbakekreving", null),
  I1("Motregning", null),
  K1("Ettergivelse", null),
  K2("Direkte oppgjør (innbetalt beløp)", null),
  K3("Tilbakekreving ettergivelse", null);

}

@Schema(description = "Konteringstypen er NY for nye konteringer for en stønad i en periode. " +
    "Deretter skal alle konteringer for samme stønad i samme periode markere ENDRING, altså B3-konteringen og for alle påfølgende B1-konteringer.")
enum class Type {
  NY, ENDRING
}

enum class Justering {
  INDEKSREGULERING, ALDERSJUSTERING
}

enum class GebyrRolle {
  BIDRAGSMOTTAKER, BIDRAGSPLIKTIG
}