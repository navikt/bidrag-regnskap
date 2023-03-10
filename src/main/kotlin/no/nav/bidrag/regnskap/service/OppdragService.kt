package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.oppdrag.OppdragResponse
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val LOGGER = LoggerFactory.getLogger(OppdragService::class.java)

@Service
class OppdragService(
  private val persistenceService: PersistenceService,
  private val oppdragsperiodeService: OppdragsperiodeService,
  private val konteringService: KonteringService
) {

  fun hentOppdrag(oppdragId: Int): OppdragResponse? {
    val oppdrag = persistenceService.hentOppdrag(oppdragId) ?: return null

    return OppdragResponse(
      oppdragId = oppdrag.oppdragId,
      type = oppdrag.stønadType,
      sakId = oppdrag.sakId,
      kravhaverIdent = oppdrag.kravhaverIdent,
      skyldnerIdent = oppdrag.skyldnerIdent,
      endretTidspunkt = oppdrag.endretTidspunkt.toString(),
      engangsbelopId = oppdrag.engangsbeløpId,
      oppdragsperioder = oppdragsperiodeService.hentOppdragsperioderMedKonteringer(oppdrag)
    )
  }

  @Transactional
  fun lagreHendelse(hendelse: Hendelse): Int {
    val oppdrag = hentOppdrag(hendelse)

    return lagreEllerOppdaterOppdrag(oppdrag, hendelse)
  }

  fun lagreEllerOppdaterOppdrag(hentetOppdrag: Oppdrag?, hendelse: Hendelse): Int {
    val erOppdatering = hentetOppdrag != null
    val oppdrag = hentetOppdrag ?: opprettOppdrag(hendelse)
    val sisteOverførtePeriode = persistenceService.finnSisteOverførtePeriode()

    if (hendelse.endretEngangsbeløpId != null) {
      oppdrag.engangsbeløpId = hendelse.engangsbeløpId
    }

    hendelse.periodeListe.forEach { periode ->
      if (periode.beløp != null) {
        val oppdragsperiode = oppdragsperiodeService.opprettNyOppdragsperiode(hendelse, periode, oppdrag)
        if (erOppdatering) {
          konteringService.opprettKorreksjonskonteringer(oppdrag, oppdragsperiode, sisteOverførtePeriode)
        }
        oppdragsperiodeService.settAktivTilDatoPåEksisterendeOppdragsperioder(oppdrag, oppdragsperiode.periodeFra)
        oppdrag.oppdragsperioder = oppdrag.oppdragsperioder.plus(oppdragsperiode)
        konteringService.opprettNyeKonteringerPåOppdragsperiode(
          oppdragsperiode,
          hendelse,
          sisteOverførtePeriode
        )
      } else {
        konteringService.opprettKorreksjonskonteringer(oppdrag, periode, sisteOverførtePeriode)
        oppdragsperiodeService.settAktivTilDatoPåEksisterendeOppdragsperioder(oppdrag, periode.periodeFomDato)
      }
    }

    oppdatererVerdierPåOppdrag(hendelse, oppdrag)
    val oppdragId = persistenceService.lagreOppdrag(oppdrag)

    LOGGER.info("Oppdrag med ID: $oppdragId er ${if (erOppdatering) "oppdatert." else "opprettet."}")

    return oppdragId
  }

  private fun hentOppdrag(hendelse: Hendelse): Oppdrag? {
    if (hendelse.endretEngangsbeløpId != null) {
      return persistenceService.hentOppdragPåEngangsbeløpId(hendelse.endretEngangsbeløpId)
    } else if (hendelse.engangsbeløpId == null) {
      return persistenceService.hentOppdragPaUnikeIdentifikatorer(
        hendelse.type,
        hendelse.kravhaverIdent,
        hendelse.skyldnerIdent,
        hendelse.sakId
      )
    }
    return null
  }

  private fun opprettOppdrag(hendelse: Hendelse): Oppdrag {
    LOGGER.info("Fant ikke eksisterende oppdrag for vedtakID: ${hendelse.vedtakId}. Opprettet nytt oppdrag..")
    return Oppdrag(
      stønadType = hendelse.type,
      sakId = hendelse.sakId,
      kravhaverIdent = hendelse.kravhaverIdent,
      skyldnerIdent = hendelse.skyldnerIdent,
      utsattTilDato = hendelse.utsattTilDato,
      engangsbeløpId = hendelse.engangsbeløpId
    )
  }

  private fun oppdatererVerdierPåOppdrag(hendelse: Hendelse, oppdrag: Oppdrag) {
    oppdrag.endretTidspunkt = LocalDateTime.now()
    oppdrag.utsattTilDato = hendelse.utsattTilDato
  }
}