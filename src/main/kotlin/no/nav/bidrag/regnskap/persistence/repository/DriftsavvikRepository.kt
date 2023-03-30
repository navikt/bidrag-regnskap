package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Driftsavvik
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface DriftsavvikRepository : JpaRepository<Driftsavvik, Int> {

    fun findAllByTidspunktTilAfterOrTidspunktTilIsNull(tidspunktTil: LocalDateTime): List<Driftsavvik>

    fun findByPåløpId(påløpId: Int): Driftsavvik?
}
