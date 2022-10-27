package no.nav.bidrag.regnskap.service

import no.nav.bidrag.behandling.felles.enums.EngangsbelopType
import no.nav.bidrag.behandling.felles.enums.VedtakType
import no.nav.bidrag.regnskap.dto.Justering
import no.nav.bidrag.regnskap.dto.KonteringResponse
import no.nav.bidrag.regnskap.dto.Transaksjonskode
import no.nav.bidrag.regnskap.dto.Type
import no.nav.bidrag.regnskap.hendelse.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors
import java.util.stream.Stream

@Service
class KonteringService(
  private val persistenceService: PersistenceService
) {

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

  fun opprettNyeKonteringerPaOppdragsperioder(oppdragsperioder: List<Oppdragsperiode>, hendelse: Hendelse, oppdatering: Boolean = false) {
    oppdragsperioder.forEachIndexed { indexOppdragsperiode, oppdragsperiode ->
      val konteringListe = mutableListOf<Kontering>()

      val perioderForOppdrasperiode = hentAllePeriodeForOppdragsperiodeForOverførtePeriode(oppdragsperiode)

      perioderForOppdrasperiode.forEachIndexed { indexPeriode, periode ->
        konteringListe.add(
          Kontering(
            transaksjonskode = Transaksjonskode.hentTransaksjonskodeForType(hendelse.type).name,
            overforingsperiode = periode.toString(),
            type = vurderOmNyEllerEndring(
              indexOppdragsperiode, indexPeriode, oppdatering
            ),
            justering = vurderOmJustinger(hendelse),
            gebyrRolle = vurderOmGebyrRolle(hendelse),
            oppdragsperiode = oppdragsperiode
          )
        )
      }
      oppdragsperiode.konteringer = konteringListe
    }
  }

  fun opprettKorreksjonskonteringerForAlleredeOversendteKonteringer(oppdrag: Oppdrag, nyeOppdragsperioder: List<Oppdragsperiode>) {
    val overforteKonteringerListe = finnAlleOverforteKontering(oppdrag)

    nyeOppdragsperioder.forEach { nyOppdragsperiode ->
      val perioderForNyOppdrasperiode = hentAllePerioderForOppdragsperiode(nyOppdragsperiode)

      overforteKonteringerListe.forEach { kontering ->
        val korreksjonskode = Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode
        if (perioderForNyOppdrasperiode.contains(YearMonth.parse(kontering.overforingsperiode)) && korreksjonskode != null && !erOverfortKonteringAlleredeKorrigert(
            kontering, overforteKonteringerListe
          )
        ) {
          persistenceService.lagreKontering(
            Kontering(
              oppdragsperiode = kontering.oppdragsperiode,
              overforingsperiode = kontering.overforingsperiode,
              transaksjonskode = korreksjonskode,
              type = Type.ENDRING.toString(),
              justering = kontering.justering,
              gebyrRolle = kontering.gebyrRolle
            )
          )
        }
      }
    }
  }

  private fun vurderOmNyEllerEndring(indexOppdragsperiode: Int, indexPeriode: Int, oppdatering: Boolean): String {
    return if (indexOppdragsperiode == 0 && indexPeriode == 0 && !oppdatering) Type.NY.toString() else Type.ENDRING.toString()
  }

  private fun vurderOmJustinger(hendelse: Hendelse): String? {
    return when (hendelse.vedtakType) {
      VedtakType.AUTOMATISK_INDEKSREGULERING -> Justering.INDEKSREGULERING.name
      VedtakType.AUTOMATISK_REVURDERING_FORSKUDD_11_AAR -> Justering.ALDERSJUSTERING.name
      else -> null
    }
  }

  private fun vurderOmGebyrRolle(hendelse: Hendelse): String? {
    return when (hendelse.type) {
      EngangsbelopType.GEBYR_SKYLDNER.name -> "BIDRAGSPLIKTIG"
      EngangsbelopType.GEBYR_MOTTAKER.name -> "BIDRAGSMOTTAKER"
      else -> null
    }
  }

  private fun hentAllePeriodeForOppdragsperiodeForOverførtePeriode(oppdragsperiode: Oppdragsperiode): List<YearMonth> {
    val perioderForOppdrasperiode = hentAllePerioderForOppdragsperiode(oppdragsperiode)
    val sisteOverfortePeriode = persistenceService.finnSisteOverfortePeriode()

    //Filtrer etter det ut alle perioder som er senere enn siste overførte periode da disse konteringene ikke skal opprettes før påløpsfilen genereres.
    return perioderForOppdrasperiode.filter { it.isBefore(sisteOverfortePeriode.plusMonths(1)) }
  }

  private fun hentAllePerioderForOppdragsperiode(oppdragsperiode: Oppdragsperiode): List<YearMonth> {
    var periodeTil = oppdragsperiode.periodeTil
    val sisteOverfortePeriode = persistenceService.finnSisteOverfortePeriode()

    if (periodeTil == null) {
      periodeTil = LocalDate.of(
        sisteOverfortePeriode.year, sisteOverfortePeriode.month, 1
      ).plusMonths(1)
    }

    // Finner alle perioder som er mellom fra og med periodeFra og til og med periodeTil (Om den eksisterer, ellers brukes siste overførte periode)
    return Stream.iterate(oppdragsperiode.periodeFra) { date: LocalDate -> date.plusMonths(1) }.limit(
      ChronoUnit.MONTHS.between(
        oppdragsperiode.periodeFra, periodeTil
      )
    ).map { it.format(DateTimeFormatter.ofPattern("yyyy-MM")) }.map { YearMonth.parse(it) }.collect(Collectors.toList())
  }

  private fun erOverfortKonteringAlleredeKorrigert(
    kontering: Kontering, overforteKonteringerListe: List<Kontering>
  ): Boolean {
    if (overforteKonteringerListe.any {
        it.transaksjonskode == Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode && it.oppdragsperiode == kontering.oppdragsperiode && it.overforingsperiode == kontering.overforingsperiode
      }) {
      return true
    }
    return false
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