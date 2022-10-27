package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.OppdragResponse
import no.nav.bidrag.regnskap.hendelse.krav.SendKravQueue
import no.nav.bidrag.regnskap.hendelse.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

private val LOGGER = LoggerFactory.getLogger(OppdragService::class.java)

@Service
class OppdragService(
  val persistenceService: PersistenceService,
  val oppdragsperiodeService: OppdragsperiodeService,
  val konteringService: KonteringService,
  val sendKravQueue: SendKravQueue
) {

  fun hentOppdrag(oppdragId: Int): OppdragResponse {
    val oppdrag = persistenceService.hentOppdrag(oppdragId).get()

    return OppdragResponse(
      oppdragId = oppdrag.oppdragId,
      type = oppdrag.stonadType,
      kravhaverIdent = oppdrag.kravhaverIdent,
      skyldnerIdent = oppdrag.skyldnerIdent,
      referanse = oppdrag.eksternReferanse,
      sistOversendtePeriode = oppdrag.sistOversendtePeriode,
      endretTidspunkt = oppdrag.endretTidspunkt.toString(),
      engangsbelopId = oppdrag.engangsbelopId,
      oppdragsperioder = oppdragsperiodeService.hentOppdragsperioderMedKonteringer(oppdrag)
    )
  }

  @Transactional
  fun lagreHendelse(hendelse: Hendelse): Int {
    val oppdragOptional: Optional<Oppdrag>
    if (hendelse.engangsbelopId != null) {
      oppdragOptional = persistenceService.hentOppdragPaEngangsbelopId(hendelse.engangsbelopId)
    } else {
      oppdragOptional = persistenceService.hentOppdragPaUnikeIdentifikatorer(
        hendelse.type, hendelse.kravhaverIdent, hendelse.skyldnerIdent, hendelse.eksternReferanse
      )
    }

    return if (oppdragOptional.isPresent) {
      LOGGER.debug(
        "Kombinasjonen av stonadType, kravhaverIdent, skyldnerIdent og referanse viser til et allerede opprettet oppdrag med id: ${oppdragOptional.get().oppdragId}" + "\nOppdaterer oppdrag.."
      )
      oppdaterOppdrag(hendelse, oppdragOptional.get())
    } else {
      LOGGER.debug(
        "Kombinasjonen av stonadType, kravhaverIdent, skyldnerIdent og referanse viser ikke til et eksisterende oppdrag. " + "\nOpprettet nytt oppdrag.."
      )
      opprettNyttOppdrag(hendelse)
    }
  }

  fun opprettNyttOppdrag(
    hendelse: Hendelse
  ): Int {
    val oppdrag = Oppdrag(
      stonadType = hendelse.type,
      kravhaverIdent = hendelse.kravhaverIdent,
      skyldnerIdent = hendelse.skyldnerIdent,
      eksternReferanse = hendelse.eksternReferanse,
      utsattTilDato = hendelse.utsattTilDato,
      engangsbelopId = hendelse.engangsbelopId
    )

    val oppdragsperioder = oppdragsperiodeService.opprettNyeOppdragsperioder(hendelse, oppdrag)
    konteringService.opprettNyeKonteringerPaOppdragsperioder(oppdragsperioder, hendelse)

    oppdrag.oppdragsperioder = oppdragsperioder

    val oppdragId = persistenceService.lagreOppdrag(oppdrag)!!

    sendKravQueue.leggTil(oppdragId)

    return oppdragId
  }

  fun oppdaterOppdrag(
    hendelse: Hendelse, oppdrag: Oppdrag
  ): Int {
    val nyeOppdragsperioder = oppdragsperiodeService.opprettNyeOppdragsperioder(hendelse, oppdrag)
    konteringService.opprettKorreksjonskonteringerForAlleredeOversendteKonteringer(oppdrag, nyeOppdragsperioder)
    konteringService.opprettNyeKonteringerPaOppdragsperioder(
      nyeOppdragsperioder, hendelse, true
    )

    //TODO: Håndtere oppdatering av skyldnerIdent/kravhaver, egentlig alt som ligger på oppdrag nivå.

    oppdrag.oppdragsperioder = nyeOppdragsperioder
    oppdrag.endretTidspunkt = LocalDateTime.now()

    val oppdragId = persistenceService.lagreOppdrag(oppdrag)!!
    sendKravQueue.leggTil(oppdragId)

    return oppdragId
  }
}