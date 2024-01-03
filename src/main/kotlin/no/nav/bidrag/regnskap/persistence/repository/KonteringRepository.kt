package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Kontering
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.time.LocalDateTime

interface KonteringRepository : JpaRepository<Kontering, Int> {

    fun findAllByOverføringstidspunktIsNull(): List<Kontering>

    fun findAllByOverføringsperiodeOrOverføringstidspunktIsNull(overforingsperiode: String): List<Kontering>

    fun findAllByOverføringstidspunktIsNullOrderByKonteringId(pageable: Pageable): Page<Kontering>

    @Query(
        value = "SELECT * FROM konteringer WHERE date(overforingstidspunkt) = ?1",
        nativeQuery = true,
    )
    fun hentAlleKonteringerForDato(dato: LocalDate): List<Kontering>

    @Query(
        value = "SELECT * FROM konteringer WHERE date(overforingstidspunkt) = ?1 AND overforingstidspunkt >= ?2 AND overforingstidspunkt < ?3",
        nativeQuery = true,
    )
    fun hentAlleKonteringerForDato(dato: LocalDate, fomTidspunkt: LocalDateTime, tomTidspunkt: LocalDateTime): List<Kontering>

    fun findAllByBehandlingsstatusOkTidspunktIsNullAndOverføringstidspunktIsNotNullAndSisteReferansekodeIsNotNull(): List<Kontering>

    fun findAllByBehandlingsstatusOkTidspunktIsNullAndOverføringstidspunktIsNotNullAndSisteReferansekodeIsIn(
        sisteReferansekoder: List<String>,
    ): List<Kontering>
}
