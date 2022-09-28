package no.nav.bidrag.regnskap.service

import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.repository.KonteringRepository
import no.nav.bidrag.regnskap.persistence.repository.OppdragRepository
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import no.nav.bidrag.regnskap.persistence.repository.OverforingKonteringRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(PersistenceService::class.java)

@Service
class PersistenceService(
  val oppdragRepository: OppdragRepository,
  val oppdragsperiodeRepository: OppdragsperiodeRepository,
  val konteringRepository: KonteringRepository,
  val overforingKonteringRepository: OverforingKonteringRepository
) {

  fun hentOppdrag(oppdragId: Int): Optional<Oppdrag> {
    LOGGER.info("Henter oppdrag med ID: $oppdragId")
    return oppdragRepository.findById(oppdragId)
  }

  fun hentOppdragPaUnikeIdentifikatorer(
    sakId: Int, stonadType: StonadType, kravhaverIdent: String, skyldnerIdent: String, referanse: String?
  ): Optional<Oppdrag> {
    SECURE_LOGGER.info(
      "Henter oppdrag med stonadType: $stonadType, kravhaverIdent: $kravhaverIdent, skyldnerIdent: $skyldnerIdent, referanse: $referanse"
    )
    return oppdragRepository.findByStonadTypeAndKravhaverIdentAndSkyldnerIdentAndReferanse(
      stonadType.toString(), kravhaverIdent, skyldnerIdent, referanse
    )
  }

  fun lagreOppdragsperiode(oppdragsperiode: Oppdragsperiode) {
    oppdragsperiodeRepository.save(oppdragsperiode)
  }

  fun lagreOppdrag(oppdrag: Oppdrag): Int? {
    val nyttOppdrag = oppdragRepository.save(oppdrag)
    LOGGER.info("Lagret oppdrag med ID: ${nyttOppdrag.oppdragId}")
    return nyttOppdrag.oppdragId
  }
}