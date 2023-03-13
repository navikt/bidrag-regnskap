package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import org.springframework.data.jpa.repository.JpaRepository

interface OppdragRepository : JpaRepository<Oppdrag, Int> {
  fun findByStønadTypeAndKravhaverIdentAndSkyldnerIdentAndSakId(
    stønadType: String,
    kravhaverIdent: String?,
    skyldnerIdent: String,
    sakId: String
  ): Oppdrag?

}