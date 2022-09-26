package no.nav.bidrag.regnskap.service

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

  fun hentOppdragsperiodePaOppdragsperiodeId(oppdragsperiodeId: Int): List<Oppdragsperiode> {
    LOGGER.info("Henter aktive oppdragsperioder på oppdragsID: $oppdragsperiodeId")
    return oppdragsperiodeRepository.findAllByOppdragsperiodeId(oppdragsperiodeId)
  }

  fun lagreOppdrag(oppdrag: Oppdrag): Int? {
    val nyttOppdrag = oppdragRepository.save(oppdrag)
    LOGGER.info("Lagret oppdrag med ID: ${nyttOppdrag.oppdragId}")
    return nyttOppdrag.oppdragId
  }
}