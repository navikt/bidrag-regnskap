package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface OppdragRepository : JpaRepository<Oppdrag, Int> {

    @Query(
        """ SELECT o
            FROM oppdrag o
            JOIN FETCH o.oppdragsperioder
            WHERE o.stønadType = :stønadType 
                AND o.kravhaverIdent = :kravhaverIdent 
                AND o.skyldnerIdent = :skyldnerIdent 
                AND o.sakId = :sakId
        """,
    )
    fun finnOppdragMedStønadTypeKravhanderSkylderOgSaksnummer(
        stønadType: String,
        kravhaverIdent: String?,
        skyldnerIdent: String,
        sakId: String,
    ): Oppdrag?

    fun findAllBySakIdIs(sakId: String): List<Oppdrag>

    fun findAllByKravhaverIdent(kravhaverIdent: String): List<Oppdrag>

    fun findAllBySakIdAndKravhaverIdent(sakId: String, kravhaverIdent: String): List<Oppdrag>

    fun findAllByMottakerIdent(mottakerIdent: String): List<Oppdrag>

    fun findAllBySkyldnerIdent(skyldnerIdent: String): List<Oppdrag>

    fun findAllByGjelderIdent(gjelderIdent: String): List<Oppdrag>

    fun findAllByUtsattTilDatoIsNotNullAndUtsattTilDatoIsAfter(utsattTilDato: LocalDate): List<Oppdrag>
}
