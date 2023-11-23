package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import org.springframework.data.jpa.repository.JpaRepository

interface OppdragRepository : JpaRepository<Oppdrag, Int> {
    fun findByStønadTypeAndKravhaverIdentAndSkyldnerIdentAndSakId(
        stønadType: String,
        kravhaverIdent: String?,
        skyldnerIdent: String,
        sakId: String,
    ): Oppdrag?

    fun findAllBySakIdIs(sakId: String): List<Oppdrag>

    fun findAllByKravhaverIdent(kravhaverIdent: String): List<Oppdrag>

    fun findAllBySkyldnerIdent(skyldnerIdent: String): List<Oppdrag>

    fun findAllByGjelderIdent(gjelderIdent: String): List<Oppdrag>
}
