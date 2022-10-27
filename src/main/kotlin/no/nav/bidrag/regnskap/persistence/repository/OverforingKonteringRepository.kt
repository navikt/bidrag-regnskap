package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.OverforingKontering
import org.springframework.data.jpa.repository.JpaRepository

interface OverforingKonteringRepository : JpaRepository<OverforingKontering, Int> {

}