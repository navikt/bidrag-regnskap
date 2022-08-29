package no.nav.bidrag.regnskap.model

import java.time.LocalDate
import java.time.YearMonth

const val KRAV_BESKRIVELSE =
  "Operasjon for å levere krav fra NAV til regnskapet hos Skatteetaten. " +
      "Et krav består av en liste med konteringer. Det forventes at disse konteringen behandles samlet. " +
      "Det vil si at hvis én av konteringene feiler, skal ingen av konteringene i kravet benyttes.\n" +
      "\n" +
      "Dersom et krav feiler kan det forsøkes overført på nytt gjentatte ganger inntil kravet er overført. " +
      "Krav som gjelder samme fagsak må leveres i korrekt rekkefølge. " +
      "Feiler et krav i en sak, skal ikke senere krav i samme sak overføres. " +
      "Senere krav i andre saker kan overføres, selv om noen av partene fra den feilende fagsaken er involvert.\n" +
      "\n" +
      "Det forventes at et krav alltid inneholder de samme konteringene. " +
      "Dersom et nytt vedtak fører til et nytt krav som venter på et tidligere feilende krav, skal ikke konteringene fra det seneste kravet slås sammen med det ventende kravet.\n" +
      "\n" +
      "NAV har ansvar for å manuelt følge opp krav som ved flere forsøk ikke kan overføres, og vil løse opp i problemet i samarbeid med Skatteetaten.\n" +
      "\n" +
      "Ved månedlig påløp skal ikke dette grensesnittet benyttes. " +
      "Tilsvarende krav legges i stedet inn i en fil som overføres til Skatteetaten gjennom filslusa.\n" +
      "\n" +
      "Formatet på påløpsfilen skal være tilsvarende det nye grensesnittet, men hvor hvert krav legges inn på egen linje."

data class KravRequest(
  val konteringer: List<Konteringer>
)

data class Konteringer(
  val transaksjonskode: Transaksjonskode,
  val type: Type,
  val justering: Justering?,
  val gebyrRolle: GebyrRolle?,
  val gjelderIdent: String,
  val kravhaverIdent: String,
  val mottakerIdent: String,
  val skyldnerIdent: String,
  val belop: Int,
  val valuta: String,
  val periode: YearMonth,
  val vedtaksdato: LocalDate,
  val kjoredato: LocalDate,
  val saksbehandlerId: String,
  val attestantId: String,
  val tekst: String?,
  val fagsystemId: String,
  val delytelsesId: String
)

enum class Transaksjonskode(beskrivelse: String, korreksjonskode: Boolean) {
  A1("Bidragsforskudd", false),
  A3("Bidragsforskudd", true),
  B1("Underholdsbidrag (m/u tilleggsbidrag)", false),
  B3("Underholdsbidrag (m/u tilleggsbidrag)", true),
  D1("18årsbidrag", false),
  D3("18årsbidrag", true),
  E1("Bidrag til særlige utgifter (særtilskudd)", false),
  E3("Bidrag til særlige utgifter (særtilskudd)", true),
  F1("Ektefellebidrag", false),
  F3("Ektefellebidrag", true),
  G1("Gebyr", false),
  G3("Gebyr", true),
  H1("Tilbakekreving", false),
  H3("Tilbakekreving", true),
  I1("Motregning", false),
  K1("Ettergivelse", false),
  K2("Direkte oppgjør (innbetalt beløp)", false),
  K3("Tilbakekreving ettergivelse", false),
}

enum class Type {
  NY,
  ENDRING
}

enum class Justering {
  INDEKSREGULERING,
  ALDERSJUSTERING
}

enum class GebyrRolle {
  BIDRAGSMOTTAKER,
  BIDRAGSPLIKTIG
}