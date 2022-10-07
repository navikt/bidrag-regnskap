package no.nav.bidrag.regnskap.service

import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.entity.OverforingKontering
import no.nav.bidrag.regnskap.persistence.entity.Palop
import no.nav.bidrag.regnskap.persistence.repository.OppdragRepository
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import no.nav.bidrag.regnskap.persistence.repository.OverforingKonteringRepository
import no.nav.bidrag.regnskap.persistence.repository.PalopRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.*

private val LOGGER = LoggerFactory.getLogger(PersistenceService::class.java)

@Service
class PersistenceService(
  val oppdragRepository: OppdragRepository,
  val oppdragsperiodeRepository: OppdragsperiodeRepository,
  val overforingKonteringRepository: OverforingKonteringRepository,
  val palopRepository: PalopRepository
) {

  fun hentOppdrag(oppdragId: Int): Optional<Oppdrag> {
    LOGGER.info("Henter oppdrag med ID: $oppdragId")
    return oppdragRepository.findById(oppdragId)
  }

  fun hentOppdragPaUnikeIdentifikatorer(
    stonadType: StonadType, kravhaverIdent: String, skyldnerIdent: String, referanse: String?
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
    val lagretOppdrag = oppdragRepository.save(oppdrag)
    LOGGER.info("Lagret oppdrag med ID: ${lagretOppdrag.oppdragId}")
    return lagretOppdrag.oppdragId
  }

  fun lagreOverforingKontering(overforingKontering: OverforingKontering): Int? {
    val lagretOverforingKontering = overforingKonteringRepository.save(overforingKontering)
    LOGGER.info("Lagret overforingKontering med ID: ${lagretOverforingKontering.overforingId}")
    return lagretOverforingKontering.overforingId
  }

  fun hentPalop(): List<Palop> {
    return palopRepository.findAll()
  }

  fun lagrePalop(palop: Palop): Int? {
    val lagretPalop = palopRepository.save(palop)
    LOGGER.info("Lagret påløp med ID: ${lagretPalop.palopId}")
    return lagretPalop.palopId
  }

  fun finnSisteOverfortePeriode(): YearMonth {
    return YearMonth.parse(palopRepository.finnMax())
  }
}