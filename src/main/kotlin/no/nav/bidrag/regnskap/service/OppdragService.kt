package no.nav.bidrag.regnskap.service

import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.regnskap.dto.Justering
import no.nav.bidrag.regnskap.dto.KonteringResponse
import no.nav.bidrag.regnskap.dto.OppdragRequest
import no.nav.bidrag.regnskap.dto.OppdragResponse
import no.nav.bidrag.regnskap.dto.OppdragsperiodeResponse
import no.nav.bidrag.regnskap.dto.Transaksjonskode
import no.nav.bidrag.regnskap.dto.Type
import no.nav.bidrag.regnskap.exception.OppdragFinnesAlleredeException
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors
import java.util.stream.Stream

@Service
class OppdragService(
  val persistenceService: PersistenceService, val konteringService: KonteringService
) {

  fun hentOppdrag(oppdragId: Int): OppdragResponse {
    val oppdrag = persistenceService.hentOppdrag(oppdragId).get()

    return OppdragResponse(
      oppdragId = oppdrag.oppdragId,
      stonadType = StonadType.valueOf(oppdrag.stonadType),
      kravhaverIdent = oppdrag.kravhaverIdent,
      skyldnerIdent = oppdrag.skyldnerIdent,
      referanse = oppdrag.referanse,
      sistOversendtePeriode = oppdrag.sistOversendtePeriode,
      endretTidspunkt = oppdrag.endretTidspunkt,
      oppdragsperioder = hentPerioderMedKonteringer(oppdrag)
    )
  }

  @Transactional
  fun lagreOppdrag(oppdragRequest: OppdragRequest): Int {
    sjekkOmOppdragErOpprettet(oppdragRequest)

    val oppdrag = opprettOppdrag(oppdragRequest)
    val oppdragsperiode = opprettOppdragsperiode(oppdragRequest, oppdrag)
    val konteringer = opprettKonteringer(oppdragRequest, oppdragsperiode)

    oppdrag.oppdragsperioder = listOf(oppdragsperiode)
    oppdragsperiode.konteringer = konteringer

    val oppdragId = persistenceService.lagreOppdrag(oppdrag)

    return oppdragId!!
  }

  @Transactional
  fun oppdaterOppdrag(oppdragRequest: OppdragRequest): Int {
    val oppdrag = persistenceService.hentOppdragPaUnikeIdentifikatorer(
      oppdragRequest.sakId,
      oppdragRequest.stonadType,
      oppdragRequest.kravhaverIdent,
      oppdragRequest.skyldnerIdent,
      oppdragRequest.referanse
    ).get()

    //TODO: Håndtere oppdatering av skyldnerIdent/kravhaver

    val oppdragsperiode = opprettOppdragsperiode(oppdragRequest, oppdrag)

    setGamleOppdragsperioderTilInaktiv(oppdrag.oppdragsperioder, oppdragRequest)

    val konteringer = opprettKonteringer(oppdragRequest, oppdragsperiode)
    val perioderMedOverforteKonteringerListe = finnAllePerioderMedOverforteKontering(oppdrag)

    oppdragsperiode.konteringer = konteringer
    oppdrag.oppdragsperioder = arrayListOf(oppdragsperiode)

    val oppdragId = persistenceService.lagreOppdrag(oppdrag)!!

    perioderMedOverforteKonteringerListe.forEach { periode ->
      konteringService.sendKontering(oppdragId, periode)
    }

    return oppdragId
  }

  private fun sjekkOmOppdragErOpprettet(oppdragRequest: OppdragRequest) {
    val oppdragOptional = persistenceService.hentOppdragPaUnikeIdentifikatorer(
      oppdragRequest.sakId,
      oppdragRequest.stonadType,
      oppdragRequest.kravhaverIdent,
      oppdragRequest.skyldnerIdent,
      oppdragRequest.referanse
    )

    if (oppdragOptional.isPresent) {
      throw OppdragFinnesAlleredeException(
        "Kombinasjonen av stonadType, kravhaverIdent, skyldnerIdent og referanse viser til et allerede opprettet oppdrag med id: ${oppdragOptional.get().oppdragId}!",
        oppdragOptional.get())
    }
  }

  private fun finnAllePerioderMedOverforteKontering(oppdrag: Oppdrag): List<YearMonth> {
    val periodeListe = mutableListOf<YearMonth>()
    oppdrag.oppdragsperioder?.forEach { oppdragsperiode ->
      oppdragsperiode.konteringer?.forEach { kontering ->
        if (kontering.overforingstidspunkt != null) periodeListe.add(YearMonth.parse(kontering.overforingsperiode))
      }
    }
    return periodeListe
  }

  private fun setGamleOppdragsperioderTilInaktiv(oppdragsperioder: List<Oppdragsperiode>?, oppdragRequest: OppdragRequest) {
    oppdragsperioder?.forEach { oppdragsperiode ->
      oppdragsperiode.aktivTil = if (oppdragRequest.periodeFra.isBefore(oppdragsperiode.periodeFra)) oppdragsperiode.periodeFra else oppdragRequest.periodeFra
      persistenceService.lagreOppdragsperiode(oppdragsperiode) }
  }

  private fun opprettOppdrag(oppdragRequest: OppdragRequest) = Oppdrag(
    stonadType = oppdragRequest.stonadType.toString(),
    kravhaverIdent = oppdragRequest.kravhaverIdent,
    skyldnerIdent = oppdragRequest.skyldnerIdent,
    referanse = oppdragRequest.referanse,
    utsattTilDato = oppdragRequest.utsattTilDato
  )

  private fun hentPerioderMedKonteringer(oppdrag: Oppdrag): List<OppdragsperiodeResponse> {
    val oppdragsperiodeResponser = mutableListOf<OppdragsperiodeResponse>()

    (oppdrag.oppdragsperioder)?.forEach { oppdragsperiode ->
      oppdragsperiodeResponser.add(
        OppdragsperiodeResponse(
          oppdragsperiodeId = oppdragsperiode.oppdragsperiodeId,
          oppdragId = oppdragsperiode.oppdrag!!.oppdragId,
          sakId = oppdragsperiode.sakId,
          vedtakId = oppdragsperiode.vedtakId,
          gjelderIdent = oppdragsperiode.gjelderIdent,
          mottakerIdent = oppdragsperiode.mottakerIdent,
          belop = oppdragsperiode.belop,
          valuta = oppdragsperiode.valuta,
          periodeFra = oppdragsperiode.periodeFra.toString(),
          periodeTil = oppdragsperiode.periodeTil.toString(),
          vedtaksdato = oppdragsperiode.vedtaksdato.toString(),
          opprettetAv = oppdragsperiode.opprettetAv,
          delytelseId = oppdragsperiode.delytelseId,
          aktivTil = oppdragsperiode.aktivTil,
          erstatterPeriode = oppdragsperiode.erstatterPeriode,
          konteringer = hentKonteringer(oppdrag)
        )
      )
    }

    return oppdragsperiodeResponser
  }

  private fun hentKonteringer(oppdrag: Oppdrag): List<KonteringResponse> {
    val konteringResponser = mutableListOf<KonteringResponse>()

    oppdrag.oppdragsperioder?.forEach { oppdragsperiode ->
      oppdragsperiode.konteringer?.forEach { kontering ->
        konteringResponser.add(
          KonteringResponse(
            konteringId = kontering.konteringId,
            oppdragsperiodeId = kontering.oppdragsperiode!!.oppdragsperiodeId,
            transaksjonskode = Transaksjonskode.valueOf(kontering.transaksjonskode),
            overforingsperiode = kontering.overforingsperiode,
            overforingstidspunkt = kontering.overforingstidspunkt.toString(),
            type = kontering.type?.let { Type.valueOf(kontering.type) },
            justering = kontering.justering?.let { Justering.valueOf(kontering.justering) },
            gebyrRolle = kontering.gebyrRolle,
            sendtIPalopsfil = kontering.sendtIPalopsfil
          )
        )
      }
    }

    return konteringResponser
  }

  private fun opprettOppdragsperiode(oppdragRequest: OppdragRequest, oppdrag: Oppdrag): Oppdragsperiode {
    return Oppdragsperiode(
      vedtakId = oppdragRequest.vedtakId,
      sakId = oppdragRequest.sakId,
      gjelderIdent = oppdragRequest.gjelderIdent,
      mottakerIdent = oppdragRequest.mottakerIdent,
      belop = oppdragRequest.belop,
      valuta = oppdragRequest.valuta,
      periodeFra = oppdragRequest.periodeFra,
      periodeTil = oppdragRequest.periodeTil,
      vedtaksdato = oppdragRequest.vedtaksdato,
      opprettetAv = oppdragRequest.opprettetAv,
      delytelseId = oppdragRequest.delytelseId,
      tekst = oppdragRequest.tekst,
      oppdrag = oppdrag
    )
  }

  private fun opprettKonteringer(oppdragRequest: OppdragRequest, oppdragsperiode: Oppdragsperiode): List<Kontering> {
    val perioderForOppdrag = hentPerioderForOppdrag(oppdragRequest)

    val konteringsListe = mutableListOf<Kontering>()

    perioderForOppdrag.forEachIndexed { index, periode ->
      konteringsListe.add(
        Kontering(
          transaksjonskode = Transaksjonskode.A1.toString(), //TODO: Utlede denne
          overforingsperiode = periode.toString(),
          type = if (index == 0) Type.NY.toString() else Type.ENDRING.toString(),
          justering = null, //TODO: Denne må inn på en eller annen måte
          gebyrRolle = null, //TODO referanse?,
          oppdragsperiode = oppdragsperiode
        )
      )
    }
    return konteringsListe
  }

  private fun hentPerioderForOppdrag(oppdragRequest: OppdragRequest): List<YearMonth> {
    val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    return Stream.iterate(oppdragRequest.periodeFra) { date: LocalDate -> date.plusMonths(1) }
      .limit(ChronoUnit.MONTHS.between(oppdragRequest.periodeFra, oppdragRequest.periodeTil))
      .map { it.format(outputFormatter) }.map { YearMonth.parse(it) }.collect(Collectors.toList())
  }
}