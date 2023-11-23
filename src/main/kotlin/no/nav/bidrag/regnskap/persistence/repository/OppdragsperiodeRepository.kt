package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface OppdragsperiodeRepository : JpaRepository<Oppdragsperiode, Int> {

    @Query(
        """
          SELECT o.oppdragsperiodeId
            FROM oppdragsperioder o
            WHERE konteringerFullførtOpprettet = false
              AND opphørendeOppdragsperiode = false""",
    )
    @Transactional
    fun hentAlleOppdragsperioderSomIkkeHarOpprettetAlleKonteringer(): List<Int>

    fun findByReferanseAndVedtakId(referanse: String, vedtakId: Int): List<Oppdragsperiode>

    fun findAllByMottakerIdent(mottakerIdent: String): List<Oppdragsperiode>
}
