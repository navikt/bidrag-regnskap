package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OppdragRepository : JpaRepository<Oppdrag, Int> {
  fun findByStonadTypeAndKravhaverIdentAndSkyldnerIdentAndEksternReferanse(
    stonadType: String,
    kravhaverIdent: String,
    skyldnerIdent: String,
    eksternReferanse: String?
  ): Optional<Oppdrag>

  fun findByEngangsbelopId(engangsbelopId: Int): Optional<Oppdrag>

}