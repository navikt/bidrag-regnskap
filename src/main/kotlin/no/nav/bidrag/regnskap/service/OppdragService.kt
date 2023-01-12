package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.oppdrag.OppdragResponse
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val LOGGER = LoggerFactory.getLogger(OppdragService::class.java)

@Service
class OppdragService(
  val persistenceService: PersistenceService,
  val oppdragsperiodeService: OppdragsperiodeService,
  val konteringService: KonteringService
) {

  fun hentOppdrag(oppdragId: Int): OppdragResponse {
    val oppdrag = persistenceService.hentOppdrag(oppdragId) ?: error("Det finnes ingen oppdrag med angitt oppdragsId: $oppdragId")

    return OppdragResponse(
      oppdragId = oppdrag.oppdragId,
      type = oppdrag.stønadType,
      kravhaverIdent = oppdrag.kravhaverIdent,
      skyldnerIdent = oppdrag.skyldnerIdent,
      referanse = oppdrag.eksternReferanse,
      sistOversendtePeriode = oppdrag.sistOversendtePeriode,
      endretTidspunkt = oppdrag.endretTidspunkt.toString(),
      engangsbelopId = oppdrag.engangsbeløpId,
      oppdragsperioder = oppdragsperiodeService.hentOppdragsperioderMedKonteringer(oppdrag)
    )
  }

  @Transactional
  fun lagreHendelse(hendelse: Hendelse): Int {
    val oppdrag: Oppdrag? = hentOppdragOmDetFinnes(hendelse)

    return if (oppdrag != null) {
      LOGGER.debug("Fant eksisterende oppdrag med id: ${oppdrag.oppdragId}" + "\nOppdaterer oppdrag..")
      oppdaterOppdrag(hendelse, oppdrag)
    } else {
      LOGGER.debug("Fant ikke eksisterende oppdrag." + "\nOpprettet nytt oppdrag..")
      opprettNyttOppdrag(hendelse)
    }
  }

  private fun hentOppdragOmDetFinnes(hendelse: Hendelse): Oppdrag? {
    if (hendelse.endretEngangsbelopId != null) {
      return persistenceService.hentOppdragPåEngangsbeløpId(hendelse.endretEngangsbelopId)
    } else if (hendelse.engangsbelopId == null) {
      return persistenceService.hentOppdragPaUnikeIdentifikatorer(
        hendelse.type,
        hendelse.kravhaverIdent,
        hendelse.skyldnerIdent,
        hendelse.eksternReferanse //TODO() fjerne og bytte med sakID
      )
    }
    return null
  }

  fun opprettNyttOppdrag(
    hendelse: Hendelse
  ): Int {
    val oppdrag = Oppdrag(
      stønadType = hendelse.type,
      vedtakType = hendelse.vedtakType.toString(),
      kravhaverIdent = hendelse.kravhaverIdent,
      skyldnerIdent = hendelse.skyldnerIdent,
      eksternReferanse = hendelse.eksternReferanse,
      utsattTilDato = hendelse.utsattTilDato,
      engangsbeløpId = hendelse.engangsbelopId
    )

    val oppdragsperioder = oppdragsperiodeService.opprettNyeOppdragsperioder(hendelse, oppdrag)
    konteringService.opprettNyeKonteringerPåOppdragsperioder(oppdragsperioder, hendelse)

    oppdrag.oppdragsperioder = oppdragsperioder

    val oppdragId = persistenceService.lagreOppdrag(oppdrag)!!

    return oppdragId
  }

  fun oppdaterOppdrag(
    hendelse: Hendelse, oppdrag: Oppdrag
  ): Int {
    //TODO: Håndtere oppdatering av skyldnerIdent/kravhaver
    if (hendelse.endretEngangsbelopId != null) {
      oppdrag.engangsbeløpId = hendelse.engangsbelopId
    }

    val nyeOppdragsperioder = oppdragsperiodeService.opprettNyeOppdragsperioder(hendelse, oppdrag)
    konteringService.opprettKorreksjonskonteringerForAlleredeOversendteKonteringer(oppdrag, nyeOppdragsperioder)
    oppdragsperiodeService.settAktivTilDatoPåEksisterendeOppdragsperioder(oppdrag, nyeOppdragsperioder)
    konteringService.opprettNyeKonteringerPåOppdragsperioder(
      nyeOppdragsperioder, hendelse, true
    )

    oppdatererVerdierPåOppdrag(hendelse, oppdrag, nyeOppdragsperioder)

    val oppdragId = persistenceService.lagreOppdrag(oppdrag)!!

    return oppdragId
  }

  private fun oppdatererVerdierPåOppdrag(hendelse: Hendelse, oppdrag: Oppdrag, nyeOppdragsperioder: List<Oppdragsperiode>) {
    oppdrag.oppdragsperioder = nyeOppdragsperioder
    oppdrag.endretTidspunkt = LocalDateTime.now()
    oppdrag.vedtakType = hendelse.vedtakType.name
    oppdrag.utsattTilDato = hendelse.utsattTilDato
  }
}