package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.persistence.entity.Driftsavvik
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.entity.OverføringKontering
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.bidrag.regnskap.persistence.repository.DriftsavvikRepository
import no.nav.bidrag.regnskap.persistence.repository.KonteringRepository
import no.nav.bidrag.regnskap.persistence.repository.OppdragRepository
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import no.nav.bidrag.regnskap.persistence.repository.OverføringKonteringRepository
import no.nav.bidrag.regnskap.persistence.repository.PåløpRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private val LOGGER = LoggerFactory.getLogger(PersistenceService::class.java)

@Service
class PersistenceService(
  val oppdragRepository: OppdragRepository,
  val overføringKonteringRepository: OverføringKonteringRepository,
  val konteringRepository: KonteringRepository,
  val påløpRepository: PåløpRepository,
  val oppdragsperiodeRepository: OppdragsperiodeRepository,
  val driftsavvikRepository: DriftsavvikRepository
) {

  fun hentOppdrag(oppdragId: Int): Oppdrag? {
    LOGGER.debug("Henter oppdrag med ID: $oppdragId")
    return oppdragRepository.findByIdOrNull(oppdragId)
  }

  fun hentOppdragPaUnikeIdentifikatorer(
    stønadType: String, kravhaverIdent: String?, skyldnerIdent: String, referanse: String?
  ): Oppdrag? {
    SECURE_LOGGER.info(
      "Henter oppdrag med stønadType: $stønadType, kravhaverIdent: $kravhaverIdent, skyldnerIdent: $skyldnerIdent, referanse: $referanse"
    )
    return oppdragRepository.findByStønadTypeAndKravhaverIdentAndSkyldnerIdentAndEksternReferanse(
      stønadType, kravhaverIdent, skyldnerIdent, referanse
    )
  }

  fun hentOppdragPåEngangsbeløpId(engangsbeløpId: Int): Oppdrag? {
    LOGGER.debug("Henter oppdrag på engangsbeløpId: $engangsbeløpId")
    return oppdragRepository.findByEngangsbeløpId(engangsbeløpId)
  }

  fun lagreOppdrag(oppdrag: Oppdrag): Int? {
    val lagretOppdrag = oppdragRepository.save(oppdrag)
    LOGGER.debug("Lagret oppdrag med ID: ${lagretOppdrag.oppdragId}")
    return lagretOppdrag.oppdragId
  }

  fun lagreOverføringKontering(overføringKontering: OverføringKontering): Int? {
    val lagretOverføringKontering = overføringKonteringRepository.save(overføringKontering)
    LOGGER.debug("Lagret overforingKontering med ID: ${lagretOverføringKontering.overføringId}")
    return lagretOverføringKontering.overføringId
  }

  fun hentOverføringKontering(pageable: Pageable): List<OverføringKontering> {
    return overføringKonteringRepository.findAll(pageable).toList()
  }
  fun hentOverføringKonteringMedFeil(pageable: Pageable): List<OverføringKontering> {
    return overføringKonteringRepository.findByFeilmeldingIsNotNull(pageable).toList()
  }

  fun hentPåløp(): List<Påløp> {
    return påløpRepository.findAll()
  }

  fun lagrePåløp(påløp: Påløp): Int? {
    val lagretPåløp = påløpRepository.save(påløp)
    LOGGER.debug("Lagret påløp med ID: ${lagretPåløp.påløpId}")
    return lagretPåløp.påløpId
  }

  fun hentIkkeKjørtePåløp(): List<Påløp> {
    LOGGER.debug("Henter alle ikke kjørte påløp.")
    return påløpRepository.findAllByFullførtTidspunktIsNull()
  }

  fun finnSisteOverførtePeriode(): YearMonth {
    LOGGER.debug("Henter siste overførte periode.")
    try {
      val sisteOverførtePeriode = YearMonth.parse(påløpRepository.finnSisteOverførtePeriodeForPåløp())
      LOGGER.debug("Siste overførte periode var: $sisteOverførtePeriode.")
      return sisteOverførtePeriode
    } catch (e: EmptyResultDataAccessException) {
      LOGGER.error("Det finnes ingen overførte påløp. Minst et påløp må være opprettet og overført før REST kan tas i bruk.")
      throw e
    }
  }

  fun hentAlleIkkeOverførteKonteringer(): List<Kontering> {
    return konteringRepository.hentAlleIkkeOverførteKonteringer()
  }

  fun hentAlleKonteringerForDato(dato: LocalDate): List<Kontering> {
    return konteringRepository.hentAlleKonteringerForDato(dato)
  }

  fun lagreKontering(kontering: Kontering): Int? {
    val lagretKontering = konteringRepository.save(kontering)
    LOGGER.debug("Lagret kontering med ID: ${lagretKontering.konteringId}")
    return lagretKontering.konteringId
  }

  fun hentAlleOppdragsperioderSomErAktiveForPeriode(periode: LocalDate): List<Oppdragsperiode> {
    return oppdragsperiodeRepository.hentAlleOppdragsperioderSomErAktiveForPeriode(periode)
  }

  fun lagreOppdragsperiode(oppdragsperiode: Oppdragsperiode): Int? {
    return oppdragsperiodeRepository.save(oppdragsperiode).oppdragsperiodeId
  }

  fun lagreDriftsavvik(driftsavvik: Driftsavvik): Int? {
    return driftsavvikRepository.save(driftsavvik).driftsavvikId
  }

  fun harAktivtDriftsavvik(): Boolean {
    return driftsavvikRepository.findAllByTidspunktTilAfterOrTidspunktTilIsNull(LocalDateTime.now()).isNotEmpty()
  }

  fun hentAlleAktiveDriftsavvik(): List<Driftsavvik> {
    return driftsavvikRepository.findAllByTidspunktTilAfterOrTidspunktTilIsNull(LocalDateTime.now())
  }

  fun hentDriftsavvik(pageable: Pageable): List<Driftsavvik> {
    return driftsavvikRepository.findAll(pageable).toList()
  }

  fun hentDriftsavvikForPåløp(påløpId: Int): Driftsavvik? {
    return driftsavvikRepository.findByPåløpId(påløpId)
  }
}