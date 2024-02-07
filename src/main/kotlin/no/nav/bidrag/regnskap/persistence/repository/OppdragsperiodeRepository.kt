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
            WHERE o.konteringerFullførtOpprettet = false
              AND o.opphørendeOppdragsperiode = false""",
    )
    @Transactional
    fun hentAlleOppdragsperioderSomIkkeHarOpprettetAlleKonteringer(): List<Int>

    @Query(
        """ SELECT o
            FROM oppdragsperioder o
            JOIN FETCH o.oppdrag
            WHERE o.oppdragsperiodeId IN :oppdragsperioder
        """,
    )
    fun hentAlleOppdragsperioderForListe(oppdragsperioder: List<Int>): List<Oppdragsperiode>

    @Query(
        """ SELECT o
            FROM oppdragsperioder o
            JOIN FETCH o.oppdrag
            WHERE o.referanse = :referanse AND o.vedtakId = :vedtakId
        """,
    )
    fun hentOppdragPåReferanseOgVedtakId(referanse: String, vedtakId: Int): List<Oppdragsperiode>
}
