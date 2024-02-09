package no.nav.bidrag.regnskap.service

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.persistence.entity.Driftsavvik
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.bidrag.regnskap.persistence.repository.DriftsavvikRepository
import no.nav.bidrag.regnskap.persistence.repository.KonteringRepository
import no.nav.bidrag.regnskap.persistence.repository.OppdragRepository
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import no.nav.bidrag.regnskap.persistence.repository.PåløpRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private val LOGGER = LoggerFactory.getLogger(PersistenceService::class.java)

@Service
class PersistenceService(
    val oppdragRepository: OppdragRepository,
    val konteringRepository: KonteringRepository,
    val påløpRepository: PåløpRepository,
    val oppdragsperiodeRepository: OppdragsperiodeRepository,
    val driftsavvikRepository: DriftsavvikRepository,
) {

    fun hentOppdrag(oppdragId: Int): Oppdrag? {
        LOGGER.debug("Henter oppdrag med ID: $oppdragId")
        return oppdragRepository.findByIdOrNull(oppdragId)
    }

    fun hentAlleOppdragPåSakId(sakId: String): List<Oppdrag> {
        LOGGER.debug("Henter alle oppdrag med sakId: $sakId")
        return oppdragRepository.findAllBySakIdIs(sakId)
    }

    fun hentOppdragPaUnikeIdentifikatorer(stønadType: String, kravhaverIdent: String?, skyldnerIdent: String, sakId: String): Oppdrag? {
        SECURE_LOGGER.debug(
            "Henter oppdrag med stønadType: $stønadType, kravhaverIdent: $kravhaverIdent, skyldnerIdent: $skyldnerIdent, sakId: $sakId",
        )
        return oppdragRepository.finnOppdragMedStønadTypeKravhanderSkylderOgSaksnummer(
            stønadType,
            kravhaverIdent,
            skyldnerIdent,
            sakId,
        )
    }

    fun hentOppdragPåReferanseOgOmgjørVedtakId(referanse: String, omgjørVedtakId: Int): Oppdrag? {
        LOGGER.debug("Henter oppdrag på referanse: $referanse og omgjørVedtakId: $omgjørVedtakId")
        return oppdragsperiodeRepository.hentOppdragPåReferanseOgVedtakId(referanse, omgjørVedtakId).firstOrNull()?.oppdrag
    }

    fun lagreOppdrag(oppdrag: Oppdrag): Int {
        val lagretOppdrag = oppdragRepository.save(oppdrag)
        LOGGER.debug("Lagret oppdrag med ID: ${lagretOppdrag.oppdragId}")
        return lagretOppdrag.oppdragId
    }

    fun lagreOppdrag(oppdrag: List<Oppdrag>): List<Int> {
        val lagredeOppdrag = oppdragRepository.saveAll(oppdrag)
        LOGGER.debug("Lagret alle oppdrag med ID: {}", lagredeOppdrag.map { it.oppdragId })
        return lagredeOppdrag.map { it.oppdragId }
    }

    fun hentOppdragPåSaksnummerOgKravhaver(saksnummer: Saksnummer, kravhaver: Personident): List<Oppdrag> {
        return oppdragRepository.findAllBySakIdAndKravhaverIdent(saksnummer.verdi, kravhaver.verdi)
    }

    fun hentAlleMottakereMedIdent(ident: String): List<Oppdrag> {
        return oppdragRepository.findAllByMottakerIdent(ident)
    }

    fun hentAlleUtsatteOppdrag(): List<Oppdrag> {
        val now = LocalDate.now()
        return oppdragRepository.findAllByUtsattTilDatoIsNotNullAndUtsattTilDatoIsAfter(now)
    }

    fun hentPåløp(): List<Påløp> {
        return påløpRepository.findAll()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun registrerPåløpStartet(påløpId: Int, startetTidspunkt: LocalDateTime = LocalDateTime.now()) {
        val påløp = påløpRepository.findById(påløpId).get()
        påløpRepository.save(påløp.copy(startetTidspunkt = startetTidspunkt))
    }

    fun lagrePåløp(påløp: Påløp): Int {
        val lagretPåløp = påløpRepository.save(påløp)
        LOGGER.debug("Lagret påløp med ID: ${lagretPåløp.påløpId}")
        return lagretPåløp.påløpId
    }

    fun hentIkkeKjørtePåløp(): List<Påløp> {
        LOGGER.debug("Henter alle ikke kjørte påløp.")
        return påløpRepository.findAllByFullførtTidspunktIsNull()
    }

    @Cacheable(value = ["siste_overforte_periode_cache"])
    fun finnSisteOverførtePeriode(): YearMonth {
        LOGGER.debug("Henter siste overførte periode.")
        try {
            val sisteOverførtePeriode = YearMonth.parse(påløpRepository.finnSisteOverførtePeriodeForPåløp() ?: "1001-01")
            LOGGER.debug("Siste overførte periode var: {}.", sisteOverførtePeriode)
            return sisteOverførtePeriode
        } catch (e: EmptyResultDataAccessException) {
            LOGGER.error("Det finnes ingen overførte påløp. Minst et påløp må være opprettet og overført før REST kan tas i bruk.")
            throw e
        }
    }

    fun hentAlleIkkeOverførteKonteringer(): List<Kontering> {
        return konteringRepository.findAllByOverføringstidspunktIsNull()
    }

    fun hentAlleKonteringerForPeriodeOgSomIkkeErOverførtEnda(periode: String): List<Kontering> {
        return konteringRepository.findAllByOverføringsperiodeOrOverføringstidspunktIsNull(periode)
    }

    fun hentAlleKonteringerUtenBehandlingsstatusOk(): List<Kontering> {
        return konteringRepository.findAllByBehandlingsstatusOkTidspunktIsNullAndOverføringstidspunktIsNotNullAndSisteReferansekodeIsNotNull()
    }

    fun hentKonteringerUtenBehandlingsstatusOkForReferansekode(sisteReferansekoder: List<String>): List<Kontering> {
        return konteringRepository.findAllByBehandlingsstatusOkTidspunktIsNullAndOverføringstidspunktIsNotNullAndSisteReferansekodeIsIn(
            sisteReferansekoder,
        )
    }

    fun hentAlleKonteringerForDato(dato: LocalDate): List<Kontering> {
        return konteringRepository.hentAlleKonteringerForDato(dato)
    }

    fun hentAlleKonteringerForDato(dato: LocalDate, fomTidspunkt: LocalDateTime, tomTidspunkt: LocalDateTime): List<Kontering> {
        return konteringRepository.hentAlleKonteringerForDato(dato, fomTidspunkt, tomTidspunkt)
    }

    fun lagreKontering(kontering: Kontering): Int {
        val lagretKontering = konteringRepository.save(kontering)
        LOGGER.debug("Lagret kontering med ID: ${lagretKontering.konteringId}")
        return lagretKontering.konteringId
    }

    fun lagreKonteringer(konteringer: List<Kontering>) {
        konteringRepository.saveAll(konteringer)
    }

    fun lagreOppdragsperiode(oppdragsperiode: Oppdragsperiode): Int {
        val startTime = System.currentTimeMillis()
        try {
            return oppdragsperiodeRepository.save(oppdragsperiode).oppdragsperiodeId
        } finally {
            LOGGER.debug("TIDSBRUK lagreOppdragsperiode: {}ms", System.currentTimeMillis() - startTime)
        }
    }

    fun lagreOppdragsperioder(oppdragsperioder: List<Oppdragsperiode>) {
        oppdragsperiodeRepository.saveAll(oppdragsperioder)
    }

    @CacheEvict(value = ["driftsaavik_cache"], allEntries = true)
    fun lagreDriftsavvik(driftsavvik: Driftsavvik): Int {
        return driftsavvikRepository.save(driftsavvik).driftsavvikId
    }

    @Cacheable(value = ["driftsaavik_cache"], key = "#root.methodName")
    fun harAktivtDriftsavvik(): Boolean {
        return driftsavvikRepository.findAllByTidspunktTilAfterOrTidspunktTilIsNull(LocalDateTime.now()).isNotEmpty()
    }

    fun hentAlleAktiveDriftsavvik(): List<Driftsavvik> {
        return driftsavvikRepository.findAllByTidspunktTilAfterOrTidspunktTilIsNull(LocalDateTime.now())
    }

    fun hentFlereDriftsavvik(pageable: Pageable): List<Driftsavvik> {
        return driftsavvikRepository.findAll(pageable).toList()
    }

    fun hentDriftsavvik(driftsavvikId: Int): Driftsavvik? {
        return driftsavvikRepository.findByIdOrNull(driftsavvikId)
    }

    fun hentDriftsavvikForPåløp(påløpId: Int): Driftsavvik? {
        return driftsavvikRepository.findByPåløpId(påløpId)
    }

    fun hentAlleKravhavereMedIdent(ident: String): List<Oppdrag> {
        return oppdragRepository.findAllByKravhaverIdent(ident)
    }

    fun hentAlleSkyldnereMedIdent(ident: String): List<Oppdrag> {
        return oppdragRepository.findAllBySkyldnerIdent(ident)
    }

    fun hentAlleGjelderMedIdent(ident: String): List<Oppdrag> {
        return oppdragRepository.findAllByGjelderIdent(ident)
    }
}
