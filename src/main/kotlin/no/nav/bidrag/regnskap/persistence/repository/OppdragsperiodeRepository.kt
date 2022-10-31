package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface OppdragsperiodeRepository : JpaRepository<Oppdragsperiode, Int> {

  @Query(
    value = "SELECT * FROM oppdragsperioder WHERE aktiv_til IS NULL OR aktiv_til > ?1", nativeQuery = true
  )
  fun hentAlleOppdragsperioderSomErAktiveForPeriode(periode: LocalDate): List<Oppdragsperiode>
}