package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OppdragRepository: JpaRepository<Oppdrag, Int> {
  fun findByStonadTypeAndKravhaverIdentAndSkyldnerIdentAndReferanse(stonadType: String, kravhaverIdent: String, skyldnerIdent: String, Referanse: String?): Optional<Oppdrag>

}