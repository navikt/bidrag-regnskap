package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.data.jpa.repository.JpaRepository

interface OppdragsperiodeRepository : JpaRepository<Oppdragsperiode, Int> {

    fun findAllByKonteringerFullførtOpprettetIsFalseOrderByOppdragAscOppdragsperiodeIdAsc(): List<Oppdragsperiode>

    fun findByReferanseAndVedtakId(referanse: String, vedtakId: Int): Oppdragsperiode?
}
