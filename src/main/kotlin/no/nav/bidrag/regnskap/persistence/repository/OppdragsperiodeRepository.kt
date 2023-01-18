package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface OppdragsperiodeRepository : JpaRepository<Oppdragsperiode, Int> {

  fun findAllByAktivTilIsNullOrAktivTilAfter(periode: LocalDate): List<Oppdragsperiode>
}