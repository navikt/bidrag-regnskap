package no.nav.bidrag.regnskap.service

import no.nav.bidrag.behandling.felles.enums.EngangsbelopType
import no.nav.bidrag.behandling.felles.enums.VedtakType
import no.nav.bidrag.regnskap.dto.enumer.Søknadstype
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.dto.enumer.Type
import no.nav.bidrag.regnskap.dto.oppdrag.KonteringResponse
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors
import java.util.stream.Stream

private val LOGGER = LoggerFactory.getLogger(KonteringService::class.java)

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
            overforingsperiode = kontering.overføringsperiode,
            overforingstidspunkt = kontering.overføringstidspunkt,
            type = Type.valueOf(kontering.type),
            soknadType = Søknadstype.valueOf(kontering.søknadType),
            sendtIPalopsfil = kontering.sendtIPåløpsfil
          )
        )
      }
    }

    return konteringResponser
  }

  fun opprettNyeKonteringerPåOppdragsperioder(
    oppdragsperioder: List<Oppdragsperiode>, hendelse: Hendelse, oppdatering: Boolean = false
  ) {
    oppdragsperioder.forEachIndexed { indexOppdragsperiode, oppdragsperiode ->
      val konteringListe = mutableListOf<Kontering>()
      val perioderForOppdrasperiode = hentAllePerioderForOppdragsperiodeForOverførtPeriode(oppdragsperiode)

      perioderForOppdrasperiode.forEachIndexed { indexPeriode, periode ->
        konteringListe.add(
          Kontering(
            transaksjonskode = Transaksjonskode.hentTransaksjonskodeForType(hendelse.type).name,
            overføringsperiode = periode.toString(),
            type = vurderOmNyEllerEndring(indexOppdragsperiode, indexPeriode, oppdatering),
            søknadType = finnSøknadsType(hendelse),
            oppdragsperiode = oppdragsperiode
          )
        )
      }
      oppdragsperiode.konteringer = konteringListe
    }
  }

  fun opprettLøpendeKonteringerPåOppdragsperioder(løpendeOppdragsperioder: List<Oppdragsperiode>, forPeriode: String) {
    løpendeOppdragsperioder.forEach { oppdragsperiode ->

      if (oppdragsperiode.konteringer?.filter { it.overføringsperiode == forPeriode }?.isNotEmpty() == true) {
        LOGGER.debug(
          "Kontering for periode: $forPeriode i oppdragsperiode: ${oppdragsperiode.oppdragsperiodeId} er allerede opprettet!" +
              "Dette kan komme av tidligere kjøring av påløpsfil som har blitt avbrutt underveis."
        )
      } else {
        persistenceService.lagreKontering(
          Kontering(
            transaksjonskode = Transaksjonskode.hentTransaksjonskodeForType(oppdragsperiode.oppdrag!!.stønadType).name,
            overføringsperiode = forPeriode,
            type = vurderType(oppdragsperiode),
            søknadType = Søknadstype.EN.name,
            oppdragsperiode = oppdragsperiode,
            sendtIPåløpsfil = true
          )
        )
      }
    }
  }

  private fun vurderType(oppdragsperiode: Oppdragsperiode): String {
    if (oppdragsperiode.oppdrag?.oppdragsperioder?.filter { it.konteringer?.isNotEmpty() == true }?.isEmpty() == true) {
      return Type.NY.name
    }
    return Type.ENDRING.name
  }


  fun opprettKorreksjonskonteringerForAlleredeOversendteKonteringer(
    oppdrag: Oppdrag,
    nyeOppdragsperioder: List<Oppdragsperiode>
  ) {
    val overførteKonteringerListe = finnAlleOverførteKontering(oppdrag)

    nyeOppdragsperioder.forEach { nyOppdragsperiode ->
      val perioderForNyOppdrasperiode = hentAllePerioderForOppdragsperiode(nyOppdragsperiode)

      overførteKonteringerListe.forEach { kontering ->
        val korreksjonskode = Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode

        if (korreksjonskode != null
          && !erOverførtKonteringAlleredeKorrigert(kontering, overførteKonteringerListe)
          && (erPeriodeOverlappende(perioderForNyOppdrasperiode, kontering)
              || slutterNyeOppdragsperiodeFørOverførteKonteringsPeriode(kontering, perioderForNyOppdrasperiode)
              || erKonteringGebyr(kontering))
        ) {
          persistenceService.lagreKontering(
            Kontering(
              oppdragsperiode = kontering.oppdragsperiode,
              overføringsperiode = kontering.overføringsperiode,
              transaksjonskode = korreksjonskode,
              type = Type.ENDRING.toString(),
              søknadType = kontering.søknadType
            )
          )
        }
      }
    }
  }

  private fun erKonteringGebyr(kontering: Kontering) =
    (kontering.søknadType == Søknadstype.FABM.name || kontering.søknadType == Søknadstype.FABP.name)

  private fun slutterNyeOppdragsperiodeFørOverførteKonteringsPeriode(
    kontering: Kontering,
    perioderForNyOppdrasperiode: List<YearMonth>
  ) = YearMonth.parse(kontering.overføringsperiode).isAfter(perioderForNyOppdrasperiode.max())

  private fun erPeriodeOverlappende(
    perioderForNyOppdrasperiode: List<YearMonth>,
    kontering: Kontering
  ) = perioderForNyOppdrasperiode.contains(YearMonth.parse(kontering.overføringsperiode))

  private fun vurderOmNyEllerEndring(indexOppdragsperiode: Int, indexPeriode: Int, oppdatering: Boolean): String {
    return if (indexOppdragsperiode == 0 && indexPeriode == 0 && !oppdatering) Type.NY.toString() else Type.ENDRING.toString()
  }

  private fun finnSøknadsType(hendelse: Hendelse): String {
    return if (hendelse.vedtakType == VedtakType.AUTOMATISK_INDEKSREGULERING) {
      Søknadstype.IN.name
    } else if (hendelse.type == EngangsbelopType.GEBYR_MOTTAKER.name) {
      Søknadstype.FABM.name
    } else if (hendelse.type == EngangsbelopType.GEBYR_SKYLDNER.name) {
      Søknadstype.FABP.name
    } else {
      Søknadstype.EN.name
    }
  }

  private fun hentAllePerioderForOppdragsperiodeForOverførtPeriode(
    oppdragsperiode: Oppdragsperiode
  ): List<YearMonth> {
    val perioderForOppdrasperiode = hentAllePerioderForOppdragsperiode(oppdragsperiode)
    val sisteOverførtePeriode = persistenceService.finnSisteOverførtePeriode()

    //Filtrer etter det ut alle perioder som er senere enn siste overførte periode da disse konteringene ikke skal opprettes før påløpsfilen genereres.
    return perioderForOppdrasperiode.filter { it.isBefore(sisteOverførtePeriode.plusMonths(1)) }
  }

  private fun hentAllePerioderForOppdragsperiode(
    oppdragsperiode: Oppdragsperiode
  ): List<YearMonth> {
    var periodeTil = oppdragsperiode.periodeTil
    val sisteOverførtePeriode = persistenceService.finnSisteOverførtePeriode()

    if (periodeTil == null) {
      periodeTil = LocalDate.of(
        sisteOverførtePeriode.year, sisteOverførtePeriode.month, 1
      ).plusMonths(1)
    }

    if (periodeTil!!.isBefore(oppdragsperiode.periodeFra)) {
      return emptyList()
    }

    // Finner alle perioder som er mellom fra og med periodeFra og til og med periodeTil (Om den eksisterer, ellers brukes siste overførte periode)
    return Stream.iterate(oppdragsperiode.periodeFra) { date: LocalDate -> date.plusMonths(1) }.limit(
      ChronoUnit.MONTHS.between(
        oppdragsperiode.periodeFra, periodeTil
      )
    ).map { it.format(DateTimeFormatter.ofPattern("yyyy-MM")) }.map { YearMonth.parse(it) }.collect(Collectors.toList())
  }

  private fun erOverførtKonteringAlleredeKorrigert(
    kontering: Kontering, overførteKonteringerListe: List<Kontering>
  ): Boolean {
    if (overførteKonteringerListe.any {
        it.transaksjonskode == Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode && it.oppdragsperiode == kontering.oppdragsperiode && it.overføringsperiode == kontering.overføringsperiode
      }) {
      return true
    }
    return false
  }

  fun finnAlleOverførteKontering(oppdrag: Oppdrag): List<Kontering> {
    val periodeListe = mutableListOf<Kontering>()
    oppdrag.oppdragsperioder?.forEach { oppdragsperiode ->
      oppdragsperiode.konteringer?.forEach { kontering ->
        if (kontering.overføringstidspunkt != null) periodeListe.add(kontering)
      }
    }
    return periodeListe
  }
}