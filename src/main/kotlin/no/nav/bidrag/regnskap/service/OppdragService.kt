package no.nav.bidrag.regnskap.service

import no.nav.bidrag.behandling.felles.enums.StonadType
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

  fun hentOppdrag(oppdragId: Int): OppdragResponse {
    val oppdrag = persistenceService.hentOppdrag(oppdragId).get()

    return OppdragResponse(
      oppdragId = oppdrag.oppdragId,
      stonadType = StonadType.valueOf(oppdrag.stonadType),
      kravhaverIdent = oppdrag.kravhaverIdent,
      skyldnerIdent = oppdrag.skyldnerIdent,
      saksId = oppdrag.sakId,
      referanse = oppdrag.referanse,
      oppdragsperioder = hentPerioderMedKonteringer(oppdrag)
    )
  }

  @Transactional
  fun lagreOppdrag(oppdragRequest: OppdragRequest): Int {
    val oppdrag = Oppdrag(
      stonadType = oppdragRequest.stonadType.toString(),
      kravhaverIdent = oppdragRequest.kravhaverIdent,
      skyldnerIdent = oppdragRequest.skyldnerIdent,
      sakId = oppdragRequest.saksId,
      referanse = oppdragRequest.referanse,
      utsattTilDato = oppdragRequest.utsattTilDato
    )

    val oppdragId = persistenceService.lagreOppdrag(oppdrag)
    val oppdragsperiode = opprettOppdragsperiode(oppdragId, oppdragRequest)
    val oppdragsperiodeId = persistenceService.lagreOppdragsperiode(oppdragsperiode)

    opprettKonteringer(oppdragsperiodeId, hentPerioderForOppdrag(oppdragRequest))

    return oppdragId!!
  }

  private fun hentPerioderMedKonteringer(oppdrag: Oppdrag): List<OppdragsperiodeResponse> {
    val oppdragsperiodeResponser = mutableListOf<OppdragsperiodeResponse>()

    (oppdrag.oppdragsperioder)?.forEach { oppdragsperiode ->
      oppdragsperiodeResponser.add(
        OppdragsperiodeResponse(
          oppdragsperiodeId = oppdragsperiode.oppdragsperiodeId,
          oppdragId = oppdragsperiode.oppdragId,
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
          aktiv = oppdragsperiode.aktiv,
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
            oppdragsperiodeId = kontering.oppdragsperiodeId,
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

  private fun opprettKonteringer(oppdragsperiodeId: Int?, perioderForOppdrag: List<YearMonth>) {
    perioderForOppdrag.forEachIndexed { index, periode ->
      persistenceService.lagreKontering(
        Kontering(
          oppdragsperiodeId = oppdragsperiodeId,
          transaksjonskode = Transaksjonskode.A1.toString(), //TODO: Utlede denne
          overforingsperiode = periode.toString(),
          type = if (index == 0) Type.NY.toString() else Type.ENDRING.toString(),
          justering = null, //TODO: Denne må inn på en eller annen måte
          gebyrRolle = null, //TODO referanse?
        )
      )
    }
  }

  private fun opprettOppdragsperiode(oppdragId: Int?, oppdragRequest: OppdragRequest): Oppdragsperiode {
    return Oppdragsperiode(
      oppdragId = oppdragId,
      vedtakId = oppdragRequest.vedtakId,
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
    )
  }

  private fun hentPerioderForOppdrag(oppdragRequest: OppdragRequest): List<YearMonth> {
    val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    return Stream.iterate(oppdragRequest.periodeFra) { date: LocalDate -> date.plusMonths(1) }
      .limit(ChronoUnit.MONTHS.between(oppdragRequest.periodeFra, oppdragRequest.periodeTil))
      .map { it.format(outputFormatter) }.map { YearMonth.parse(it) }.collect(Collectors.toList())
  }
}