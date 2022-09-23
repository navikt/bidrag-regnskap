package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.persistence.entity.Kontering
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

  fun hentOppdragsperiodePaOppdragsId(oppdragId: Int): List<Oppdragsperiode> {
    LOGGER.info("Henter oppdragsperioder på oppdragsID: $oppdragId")
    return oppdragsperiodeRepository.findAllByOppdragId(oppdragId)
  }

  fun hentOppdragsperiodePaOppdragsIdSomErAktiv(oppdragId: Int): List<Oppdragsperiode> {
    LOGGER.info("Henter aktive oppdragsperioder på oppdragsID: $oppdragId")
    return oppdragsperiodeRepository.findAllByOppdragIdAndAktivIsTrue(oppdragId)
  }

  fun lagreOppdrag(oppdrag: Oppdrag): Int {
    val nyttOppdrag = oppdragRepository.save(oppdrag)
    LOGGER.info("Lagret oppdrag med ID: ${nyttOppdrag.oppdragId}")
    return nyttOppdrag.oppdragId
  }

  fun lagreOppdragsperiode(oppdragsperiode: Oppdragsperiode): Int {
    val nyOppdragsperiode = oppdragsperiodeRepository.save(oppdragsperiode)
    LOGGER.info("Lagret oppdragsperiode med ID: ${nyOppdragsperiode.oppdragsperiodeId}")
    return nyOppdragsperiode.oppdragsperiodeId
  }

  fun hentKonteringPaPeriodeId(oppdragsperiodeId: Int): List<Kontering> {
    LOGGER.info("Henter konteringer på oppdragsperiodeID: $oppdragsperiodeId")
    return konteringRepository.findAllByOppdragsperiodeId(oppdragsperiodeId)
  }

  fun lagreKontering(kontering: Kontering) {
    val nyKontering = konteringRepository.save(kontering)
    LOGGER.info("Lagret kontering med ID: $nyKontering")
  }
}