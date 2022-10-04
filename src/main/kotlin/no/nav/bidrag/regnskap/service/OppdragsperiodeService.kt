package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.dto.OppdragRequest
import no.nav.bidrag.regnskap.dto.OppdragsperiodeResponse
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.stereotype.Service
import java.util.*

@Service
class OppdragsperiodeService(
  val persistenceService: PersistenceService,
  val konteringService: KonteringService
) {

  fun hentOppdragsperioderMedKonteringer(oppdrag: Oppdrag): List<OppdragsperiodeResponse> {
    val oppdragsperiodeResponser = mutableListOf<OppdragsperiodeResponse>()

    (oppdrag.oppdragsperioder)?.forEach { oppdragsperiode ->
      oppdragsperiodeResponser.add(
        OppdragsperiodeResponse(
          oppdragsperiodeId = oppdragsperiode.oppdragsperiodeId,
          oppdragId = oppdragsperiode.oppdrag?.oppdragId,
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
          konteringer = konteringService.hentKonteringer(oppdrag)
        )
      )
    }
    return oppdragsperiodeResponser
  }

  fun opprettNyOppdragsperiode(oppdragRequest: OppdragRequest, oppdrag: Oppdrag): Oppdragsperiode {
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
      delytelseId = oppdragRequest.delytelseId ?: genererRandomUUID(),
      tekst = oppdragRequest.tekst,
      oppdrag = oppdrag
    )
  }

  fun setGamleOppdragsperiodeTilInaktivOgOpprettNyOppdragsperiode(
    oppdragsperioder: List<Oppdragsperiode>?, oppdragRequest: OppdragRequest
  ): Oppdragsperiode {
    oppdragsperioder?.forEach { gamleOppdragsperiode ->
      if (gamleOppdragsperiode.aktivTil == null) {
        gamleOppdragsperiode.aktivTil =
          if (oppdragRequest.periodeFra.isBefore(gamleOppdragsperiode.periodeFra)) gamleOppdragsperiode.periodeFra
          else oppdragRequest.periodeFra
        persistenceService.lagreOppdragsperiode(gamleOppdragsperiode)

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
          delytelseId = oppdragRequest.delytelseId ?: genererRandomUUID(),
          tekst = oppdragRequest.tekst,
          oppdrag = gamleOppdragsperiode.oppdrag,
          erstatterPeriode = gamleOppdragsperiode.oppdragsperiodeId
        )
      }
    }
    SECURE_LOGGER.error("Fant ingen aktiv oppdragsperiode på oppdrag med " +
        "stonadType: ${oppdragRequest.stonadType}, " +
        "kravhaverIdent: ${oppdragRequest.kravhaverIdent} " +
        "skyldnerIdent: ${oppdragRequest.skyldnerIdent} " +
        "referanse: ${oppdragRequest.referanse}")
    throw IllegalStateException("Fant ingen aktiv oppdragsperiode på oppdraget.")
  }

  private fun genererRandomUUID(): String {
    return UUID.randomUUID().toString()
  }
}