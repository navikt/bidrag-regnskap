package no.nav.bidrag.regnskap.dto.enumer

import no.nav.bidrag.behandling.felles.enums.EngangsbelopType
import no.nav.bidrag.behandling.felles.enums.StonadType

enum class Transaksjonskode(val korreksjonskode: String?, val negativtBeløp: Boolean) {
  A1("A3", false),  //Bidragsforskudd
  A3(null, true),
  B1("B3", false),  //Underholdsbidrag (m/u tilleggsbidrag)
  B3(null, true),
  D1("D3", false),  //18årsbidrag
  D3(null, true),
  E1("E3", false),  //Bidrag til særlige utgifter (særtilskudd)
  E3(null, true),
  F1("F3", false),  //Ektefellebidrag
  F3(null, true),
  G1("G3", false),  //Gebyr
  G3(null, true),
  H1("H3", false),  //Tilbakekreving
  H3(null, true),
  I1(null, true),   //Motregning
  K1(null, true),   //Ettergivelse
  K2(null, true),   //Direkte oppgjør (innbetalt beløp)
  K3(null, true);  //Tilbakekreving ettergivelse

  companion object {
    fun hentTransaksjonskodeForType(type: String): Transaksjonskode {
      return when (type) {
        StonadType.FORSKUDD.name -> A1
        StonadType.BIDRAG.name -> B1
        StonadType.OPPFOSTRINGSBIDRAG.name -> B1
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