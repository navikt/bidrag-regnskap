package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.data.jpa.repository.JpaRepository

interface OppdragsperiodeRepository: JpaRepository<Oppdragsperiode, Int> {

  fun findAllByOppdragId(oppdragId: Int): List<Oppdragsperiode>

  fun findAllByOppdragIdAndAktivIsTrue(oppdragId: Int): List<Oppdragsperiode>

}