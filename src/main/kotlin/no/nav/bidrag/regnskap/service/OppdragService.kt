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

  fun hentOppdrag(oppdragId: Int): OppdragResponse? {
    val oppdrag = persistenceService.hentOppdrag(oppdragId) ?: return null

    return OppdragResponse(
      oppdragId = oppdrag.oppdragId,
      type = oppdrag.stønadType,
      sakId = oppdrag.sakId,
      kravhaverIdent = oppdrag.kravhaverIdent,
      skyldnerIdent = oppdrag.skyldnerIdent,
      referanse = oppdrag.eksternReferanse,
      endretTidspunkt = oppdrag.endretTidspunkt.toString(),
      engangsbelopId = oppdrag.engangsbeløpId,
      oppdragsperioder = oppdragsperiodeService.hentOppdragsperioderMedKonteringer(oppdrag)
    )
  }

  @Transactional
  fun lagreHendelse(hendelse: Hendelse): Int {
    val oppdragId = lagreEllerOppdaterOppdrag(hendelse)
    if (erHendelsenOpphørt(hendelse)) {
      opphørOppdrag(hendelse, oppdragId)
    }
    return oppdragId
  }

  fun lagreEllerOppdaterOppdrag(hendelse: Hendelse): Int {
    val oppdrag: Oppdrag? = hentOppdrag(hendelse)

    return if (oppdrag != null) {
      LOGGER.info("Fant eksisterende oppdrag med id: ${oppdrag.oppdragId} for vedtakID: ${hendelse.vedtakId}" + "\nOppdaterer oppdrag..")
      oppdaterOppdrag(hendelse, oppdrag)
    } else {
      LOGGER.info("Fant ikke eksisterende oppdrag for vedtakID: ${hendelse.vedtakId}." + "\nOpprettet nytt oppdrag..")
      opprettNyttOppdrag(hendelse)
    }
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

  fun opprettNyttOppdrag(
    hendelse: Hendelse
  ): Int {
    val oppdrag = Oppdrag(
      stønadType = hendelse.type,
      vedtakType = hendelse.vedtakType.toString(),
      sakId = hendelse.sakId,
      kravhaverIdent = hendelse.kravhaverIdent,
      skyldnerIdent = hendelse.skyldnerIdent,
      eksternReferanse = hendelse.eksternReferanse,
      utsattTilDato = hendelse.utsattTilDato,
      engangsbeløpId = hendelse.engangsbeløpId
    )

    val oppdragsperioder = oppdragsperiodeService.opprettNyeOppdragsperioder(hendelse, oppdrag)
    konteringService.opprettNyeKonteringerPåOppdragsperioder(oppdragsperioder, hendelse)

    oppdrag.oppdragsperioder = oppdragsperioder

    val oppdragId = persistenceService.lagreOppdrag(oppdrag)

    LOGGER.info("Oppdrag med ID: $oppdragId er opprettet.")

    return oppdragId
  }

  fun oppdaterOppdrag(
    hendelse: Hendelse, oppdrag: Oppdrag
  ): Int {
    //TODO: Håndtere oppdatering av skyldnerIdent/kravhaver
    if (hendelse.endretEngangsbeløpId != null) {
      oppdrag.engangsbeløpId = hendelse.engangsbeløpId
    }

    val nyeOppdragsperioder = oppdragsperiodeService.opprettNyeOppdragsperioder(hendelse, oppdrag)
    if (nyeOppdragsperioder.isNotEmpty()) {
      konteringService.opprettKorreksjonskonteringer(oppdrag, nyeOppdragsperioder)
      oppdragsperiodeService.settAktivTilDatoPåEksisterendeOppdragsperioder(oppdrag, nyeOppdragsperioder.first().periodeFra)
      konteringService.opprettNyeKonteringerPåOppdragsperioder(nyeOppdragsperioder, hendelse, true)

      oppdatererVerdierPåOppdrag(hendelse, oppdrag, nyeOppdragsperioder)

      persistenceService.lagreOppdrag(oppdrag)
    }
    LOGGER.info("Oppdrag med ID: ${oppdrag.oppdragId} er oppdatert.")

    return oppdrag.oppdragId
  }

  private fun oppdatererVerdierPåOppdrag(hendelse: Hendelse, oppdrag: Oppdrag, nyeOppdragsperioder: List<Oppdragsperiode>) {
    oppdrag.oppdragsperioder = oppdrag.oppdragsperioder?.plus(nyeOppdragsperioder)
    oppdrag.endretTidspunkt = LocalDateTime.now()
    oppdrag.vedtakType = hendelse.vedtakType.name
    oppdrag.utsattTilDato = hendelse.utsattTilDato
  }

  private fun erHendelsenOpphørt(hendelse: Hendelse): Boolean {
    return hendelse.periodeListe.any { it.beløp == null }
  }

//  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun opphørOppdrag(hendelse: Hendelse, oppdragId: Int) {
    LOGGER.info("Opphører oppdrag med ID: $oppdragId")

    val oppdrag = persistenceService.hentOppdrag(oppdragId)
      ?: error("Finner ikke oppdrag med ID: $oppdragId under opphør av oppdraget!")
    val opphørsPeriode = hendelse.periodeListe.first { it.beløp == null }

    konteringService.opprettKorreksjonskonteringer(oppdrag, opphørsPeriode)
    oppdragsperiodeService.settAktivTilDatoPåEksisterendeOppdragsperioder(oppdrag, opphørsPeriode.periodeFomDato)

    oppdrag.endretTidspunkt = LocalDateTime.now()
    persistenceService.lagreOppdrag(oppdrag)
    LOGGER.info("Oppdrag med ID: $oppdragId er opphørt.")
  }
}