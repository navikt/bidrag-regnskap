package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Kontering
import org.springframework.data.jpa.repository.JpaRepository

interface KonteringRepository: JpaRepository<Kontering, Int> {

}