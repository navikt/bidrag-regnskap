package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.OverføringKontering
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface OverføringKonteringRepository : JpaRepository<OverføringKontering, Int> {

  fun findByFeilmeldingIsNotNull(pageable: Pageable) : Page<OverføringKontering>

}