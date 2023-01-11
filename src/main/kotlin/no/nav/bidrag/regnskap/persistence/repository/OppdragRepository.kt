package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import org.springframework.data.jpa.repository.JpaRepository

interface OppdragRepository : JpaRepository<Oppdrag, Int> {
  fun findByStønadTypeAndKravhaverIdentAndSkyldnerIdentAndEksternReferanse(
    stønadType: String,
    kravhaverIdent: String?,
    skyldnerIdent: String,
    eksternReferanse: String?
  ): Oppdrag?

  fun findByEngangsbeløpId(engangsbeløpId: Int): Oppdrag?

}