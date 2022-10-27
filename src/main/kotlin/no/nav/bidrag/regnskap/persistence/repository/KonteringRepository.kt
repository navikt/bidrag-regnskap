package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Kontering
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface KonteringRepository : JpaRepository<Kontering, Int> {

  @Query(
    value = "SELECT * FROM konteringer WHERE overforingstidspunkt IS NULL", nativeQuery = true
  )
  fun hentAlleIkkeOverforteKonteringer(): List<Kontering>

}