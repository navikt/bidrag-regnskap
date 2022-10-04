package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.Justering
import no.nav.bidrag.regnskap.dto.KonteringResponse
import no.nav.bidrag.regnskap.dto.Transaksjonskode
import no.nav.bidrag.regnskap.dto.Type
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class KonteringService {

  fun hentKonteringer(oppdrag: Oppdrag): List<KonteringResponse> {
    val konteringResponser = mutableListOf<KonteringResponse>()

    oppdrag.oppdragsperioder?.forEach { oppdragsperiode ->
      oppdragsperiode.konteringer?.forEach { kontering ->
        konteringResponser.add(
          KonteringResponse(
            konteringId = kontering.konteringId,
            oppdragsperiodeId = kontering.oppdragsperiode?.oppdragsperiodeId,
            transaksjonskode = Transaksjonskode.valueOf(kontering.transaksjonskode),
            overforingsperiode = kontering.overforingsperiode,
            overforingstidspunkt = kontering.overforingstidspunkt.toString(),
            type = Type.valueOf(kontering.type),
            justering = kontering.justering?.let { Justering.valueOf(kontering.justering) },
            gebyrRolle = kontering.gebyrRolle,
            sendtIPalopsfil = kontering.sendtIPalopsfil
          )
        )
      }
    }

    return konteringResponser
  }

  fun opprettNyeKonteringer(
    nyePerioderForOppdrag: List<YearMonth>, oppdragsperiode: Oppdragsperiode, oppdatering: Boolean = false
  ): List<Kontering> {
    val konteringsListe = mutableListOf<Kontering>()

    nyePerioderForOppdrag.forEachIndexed { index, periode ->
      konteringsListe.add(
        Kontering(
          transaksjonskode = Transaksjonskode.A1.toString(), //TODO: Utlede denne
          overforingsperiode = periode.toString(),
          type = if (index == 0 && !oppdatering) Type.NY.toString() else Type.ENDRING.toString(),
          justering = null, //TODO: Denne må inn på en eller annen måte
          gebyrRolle = null, //TODO referanse?,
          oppdragsperiode = oppdragsperiode
        )
      )
    }
    return konteringsListe
  }

  fun opprettErstattendeKonteringer(
    overforteKonteringerListe: List<Kontering>, nyePerioderForOppdrag: List<YearMonth>
  ): List<Kontering> {
    val nyeErstattendeKonteringer = mutableListOf<Kontering>()
    overforteKonteringerListe.forEach { kontering ->
      if (nyePerioderForOppdrag.contains(YearMonth.parse(kontering.overforingsperiode))
        && Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode != null) {
        nyeErstattendeKonteringer.add(
          Kontering(
            oppdragsperiode = kontering.oppdragsperiode,
            overforingsperiode = kontering.overforingsperiode,
            transaksjonskode = Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode!!,
            type = Type.ENDRING.toString(),
            justering = kontering.justering,
            gebyrRolle = kontering.gebyrRolle
          )
        )
      }
    }
    return nyeErstattendeKonteringer
  }

  fun finnAlleOverforteKontering(oppdrag: Oppdrag): List<Kontering> {
    val periodeListe = mutableListOf<Kontering>()
    oppdrag.oppdragsperioder?.forEach { oppdragsperiode ->
      oppdragsperiode.konteringer?.forEach { kontering ->
        if (kontering.overforingstidspunkt != null) periodeListe.add(kontering)
      }
    }
    return periodeListe
  }
}