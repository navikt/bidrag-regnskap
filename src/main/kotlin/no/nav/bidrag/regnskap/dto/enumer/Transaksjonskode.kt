package no.nav.bidrag.regnskap.dto.enumer

import no.nav.bidrag.behandling.felles.enums.EngangsbelopType
import no.nav.bidrag.behandling.felles.enums.StonadType

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

  companion object {
    fun hentTransaksjonskodeForType(type: String): Transaksjonskode {
      return when (type) {
        StonadType.FORSKUDD.name -> A1
        StonadType.BIDRAG.name -> B1
        StonadType.BIDRAG18AAR.name -> D1
        StonadType.EKTEFELLEBIDRAG.name -> F1
        StonadType.MOTREGNING.name -> I1
        EngangsbelopType.SAERTILSKUDD.name -> E1
        EngangsbelopType.GEBYR_MOTTAKER.name -> G1
        EngangsbelopType.GEBYR_SKYLDNER.name -> G1
        EngangsbelopType.TILBAKEKREVING.name -> H1
        EngangsbelopType.ETTERGIVELSE.name -> K1
        EngangsbelopType.DIREKTE_OPPGJOR.name -> K2
        EngangsbelopType.ETTERGIVELSE_TILBAKEKREVING.name -> K3
        else -> throw IllegalStateException("Ugyldig type for transaksjonskode funnet!")
      }
    }
  }
}