package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.OverforingKontering
import no.nav.bidrag.regnskap.persistence.entity.Palop
import no.nav.bidrag.regnskap.persistence.repository.KonteringRepository
import no.nav.bidrag.regnskap.persistence.repository.OppdragRepository
import no.nav.bidrag.regnskap.persistence.repository.OverforingKonteringRepository
import no.nav.bidrag.regnskap.persistence.repository.PalopRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.*

private val LOGGER = LoggerFactory.getLogger(PersistenceService::class.java)

@Service
class PersistenceService(
  val oppdragRepository: OppdragRepository,
  val overforingKonteringRepository: OverforingKonteringRepository,
  val konteringRepository: KonteringRepository,
  val palopRepository: PalopRepository
) {

  fun hentOppdrag(oppdragId: Int): Optional<Oppdrag> {
    LOGGER.debug("Henter oppdrag med ID: $oppdragId")
    return oppdragRepository.findById(oppdragId)
  }

  fun hentOppdragPaUnikeIdentifikatorer(
    stonadType: String, kravhaverIdent: String, skyldnerIdent: String, referanse: String?
  ): Optional<Oppdrag> {
    SECURE_LOGGER.info(
      "Henter oppdrag med stonadType: $stonadType, kravhaverIdent: $kravhaverIdent, skyldnerIdent: $skyldnerIdent, referanse: $referanse"
    )
    return oppdragRepository.findByStonadTypeAndKravhaverIdentAndSkyldnerIdentAndEksternReferanse(
      stonadType, kravhaverIdent, skyldnerIdent, referanse
    )
  }

  fun hentOppdragPaEngangsbelopId(engangsbelopId: Int): Optional<Oppdrag> {
    LOGGER.debug("Henter oppdrag på engangsbelopId: $engangsbelopId")
    return oppdragRepository.findByEngangsbelopId(engangsbelopId)
  }

  fun lagreOppdrag(oppdrag: Oppdrag): Int? {
    val lagretOppdrag = oppdragRepository.save(oppdrag)
    LOGGER.debug("Lagret oppdrag med ID: ${lagretOppdrag.oppdragId}")
    return lagretOppdrag.oppdragId
  }

  fun lagreOverforingKontering(overforingKontering: OverforingKontering): Int? {
    val lagretOverforingKontering = overforingKonteringRepository.save(overforingKontering)
    LOGGER.debug("Lagret overforingKontering med ID: ${lagretOverforingKontering.overforingId}")
    return lagretOverforingKontering.overforingId
  }

  fun hentPalop(): List<Palop> {
    return palopRepository.findAll()
  }

  fun lagrePalop(palop: Palop): Int? {
    val lagretPalop = palopRepository.save(palop)
    LOGGER.debug("Lagret påløp med ID: ${lagretPalop.palopId}")
    return lagretPalop.palopId
  }

  fun finnSisteOverfortePeriode(): YearMonth {
    LOGGER.debug("Henter siste overforte periode.")
    try {
      val sisteOverfortePeriode = YearMonth.parse(palopRepository.finnMax())
      LOGGER.debug("Siste overforte periode var: $sisteOverfortePeriode.")
      return sisteOverfortePeriode
    } catch (e: EmptyResultDataAccessException) {
      LOGGER.error("Det finnes ingen overførte påløp. Minst et påløp må være opprettet og overført før REST kan tas i bruk.")
      throw e
    }
  }

  fun hentAlleIkkeOverforteKonteringer(): List<Kontering> {
    return konteringRepository.hentAlleIkkeOverforteKonteringer()
  }

  fun lagreKontering(kontering: Kontering): Int? {
    val lagretKontering = konteringRepository.save(kontering)
    LOGGER.debug("Lagret kontering med ID: ${lagretKontering.konteringId}")
    return lagretKontering.konteringId
  }
}