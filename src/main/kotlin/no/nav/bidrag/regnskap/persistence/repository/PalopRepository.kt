package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Palop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PalopRepository : JpaRepository<Palop, Int> {

  @Query(
    value = "SELECT max(for_periode) FROM palop WHERE fullfort_tidspunkt IS NOT NULL", nativeQuery = true
  )
  fun finnMax(): String
}