package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Påløp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PåløpRepository : JpaRepository<Påløp, Int> {

    @Query(
        value = "SELECT max(for_periode) FROM palop WHERE fullfort_tidspunkt IS NOT NULL",
        nativeQuery = true,
    )
    fun finnSisteOverførtePeriodeForPåløp(): String?

    fun findAllByFullførtTidspunktIsNull(): List<Påløp>
}
