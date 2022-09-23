package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.Justering
import no.nav.bidrag.regnskap.dto.KonteringResponse
import no.nav.bidrag.regnskap.dto.OppdragRequest
import no.nav.bidrag.regnskap.dto.OppdragResponse
import no.nav.bidrag.regnskap.dto.OppdragsperiodeResponse
import no.nav.bidrag.regnskap.dto.Transaksjonskode
import no.nav.bidrag.regnskap.dto.Type
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
  val persistenceService: PersistenceService
) {

  fun hentOppdrag(oppdragId: Int): ResponseEntity<OppdragResponse> {

    var response = ResponseEntity<OppdragResponse>(HttpStatus.NO_CONTENT)

    persistenceService.hentOppdrag(oppdragId).ifPresent { oppdrag ->
      response = ResponseEntity(
        OppdragResponse(
          oppdrag.oppdragId,
          oppdrag.stonadType,
          oppdrag.kravhaverIdent,
          oppdrag.skyldnerIdent,
          oppdrag.sakId,
          oppdrag.referanse,
          opprettListeAvPerioderMedKonteringer(oppdrag.oppdragId)
        ), HttpStatus.OK
      )
    }

    return response
  }

  private fun opprettListeAvPerioderMedKonteringer(oppdragId: Int): List<OppdragsperiodeResponse> {
    val oppdragsperiodeResponser = mutableListOf<OppdragsperiodeResponse>()

    persistenceService.hentOppdragsperiodePaOppdragsId(oppdragId).forEach { oppdragsperiode ->
      oppdragsperiodeResponser.add(
        OppdragsperiodeResponse(
          oppdragsperiode.oppdragsperiodeId,
          oppdragsperiode.oppdragId,
          oppdragsperiode.vedtakId,
          oppdragsperiode.gjelderIdent,
          oppdragsperiode.mottakerIdent,
          oppdragsperiode.belop,
          oppdragsperiode.valuta,
          oppdragsperiode.periodeFra,
          oppdragsperiode.periodeTil,
          oppdragsperiode.vedtaksdato,
          oppdragsperiode.opprettetAv,
          oppdragsperiode.delytelseId,
          oppdragsperiode.aktiv,
          oppdragsperiode.erstatterPeriode,
          opprettListeAvKonteringer(oppdragsperiode)
        )
      )
    }

    return oppdragsperiodeResponser
  }

  private fun opprettListeAvKonteringer(oppdragsperiode: Oppdragsperiode): List<KonteringResponse> {
    val konteringResponser = mutableListOf<KonteringResponse>()

    persistenceService.hentKonteringPaPeriodeId(oppdragsperiode.oppdragsperiodeId).forEach { kontering ->
      konteringResponser.add(
        KonteringResponse(
          kontering.konteringId,
          kontering.oppdragsperiodeId,
          Transaksjonskode.valueOf(kontering.transaksjonskode),
          YearMonth.parse(kontering.overforingsperiode),
          kontering.overforingstidspunkt,
          kontering.type?.let { Type.valueOf(it) },
          kontering.justering?.let { Justering.valueOf(it) },
          kontering.gebyrRolle,
          kontering.sendtIPalopsfil
        )
      )
    }

    return konteringResponser
  }

  @Transactional
  fun lagreOppdrag(oppdragRequest: OppdragRequest): ResponseEntity<Int> {
    val oppdrag = Oppdrag(
      0,
      oppdragRequest.stonadType,
      oppdragRequest.kravhaverIdent,
      oppdragRequest.skyldnerIdent,
      oppdragRequest.saksId,
      oppdragRequest.referanse,
      oppdragRequest.utsattTilDato
    )

    val oppdragId = persistenceService.lagreOppdrag(oppdrag)

    val oppdragsperiode = Oppdragsperiode(
      0,
      oppdragId,
      oppdragRequest.vedtakId,
      oppdragRequest.gjelderIdent,
      oppdragRequest.mottakerIdent,
      oppdragRequest.belop,
      oppdragRequest.valuta,
      oppdragRequest.periodeFra,
      oppdragRequest.periodeTil,
      oppdragRequest.vedtaksdato,
      oppdragRequest.opprettetAv,
      oppdragRequest.delytelseId,
      true,
      null,
      oppdragRequest.tekst
    )

    val oppdragsperiodeId = persistenceService.lagreOppdragsperiode(oppdragsperiode)

    val perioderForOppdrag = hentPerioderForOppdrag(oppdragRequest)

    perioderForOppdrag.forEachIndexed { index, periode ->
      persistenceService.lagreKontering(
        Kontering(
          konteringId = 0,
          oppdragsperiodeId = oppdragsperiodeId,
          transaksjonskode = Transaksjonskode.A1.toString(), //TODO: Utlede denne
          overforingsperiode = periode.toString(),
          overforingstidspunkt = null,
          type = if (index == 0) Type.NY.toString() else Type.ENDRING.toString(),
          justering = null, //TODO: Denne må inn på en eller annen måte
          gebyrRolle = null, //TODO referanse?
          sendtIPalopsfil = false
        )
      )
    }

    return ResponseEntity(oppdragId, HttpStatus.OK)
  }

  private fun hentPerioderForOppdrag(oppdragRequest: OppdragRequest): List<YearMonth> {
    val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    return Stream.iterate(oppdragRequest.periodeFra) { date: LocalDate -> date.plusMonths(1) }
      .limit(ChronoUnit.MONTHS.between(oppdragRequest.periodeFra, oppdragRequest.periodeTil))
      .map { it.format(outputFormatter) }.map { YearMonth.parse(it) }.collect(Collectors.toList())
  }
}