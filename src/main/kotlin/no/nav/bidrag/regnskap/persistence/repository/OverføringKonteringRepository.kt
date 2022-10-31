package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.OverføringKontering
import org.springframework.data.jpa.repository.JpaRepository

interface OverføringKonteringRepository : JpaRepository<OverføringKontering, Int> {

}