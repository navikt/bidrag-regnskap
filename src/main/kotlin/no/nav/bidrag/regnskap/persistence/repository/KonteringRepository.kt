package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Kontering
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface KonteringRepository : JpaRepository<Kontering, Int> {

    fun findAllByOverføringstidspunktIsNull(): List<Kontering>

    @Query(
        value = "SELECT * FROM konteringer WHERE date(overforingstidspunkt) = ?1",
        nativeQuery = true
    )
    fun hentAlleKonteringerForDato(dato: LocalDate): List<Kontering>

    fun findAllByBehandlingsstatusOkTidspunktIsNullAndOverføringstidspunktIsNotNullAndSisteReferansekodeIsNotNull(): List<Kontering>

    fun findAllByBehandlingsstatusOkTidspunktIsNullAndOverføringstidspunktIsNotNullAndSisteReferansekodeIsIn(sisteReferansekoder: List<String>): List<Kontering>
}
