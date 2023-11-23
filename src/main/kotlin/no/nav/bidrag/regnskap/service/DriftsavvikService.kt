package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.persistence.entity.Driftsavvik
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DriftsavvikService(
    private val persistenceService: PersistenceService,
) {

    fun hentFlereDriftsavvik(antallDriftsavvik: Int): List<Driftsavvik> {
        return persistenceService.hentFlereDriftsavvik(PageRequest.of(0, antallDriftsavvik))
    }

    fun hentAlleAktiveDriftsavvik(): List<Driftsavvik> {
        return persistenceService.hentAlleAktiveDriftsavvik()
    }

    fun lagreDriftsavvik(tidspunktFra: LocalDateTime, tidspunktTil: LocalDateTime?, opprettetAv: String?, årsak: String?): Int {
        return persistenceService.lagreDriftsavvik(
            Driftsavvik(tidspunktFra = tidspunktFra, tidspunktTil = tidspunktTil, opprettetAv = opprettetAv, årsak = årsak),
        )
    }

    @Transactional
    fun endreDriftsavvik(driftsavvikId: Int, tidspunktTil: LocalDateTime?): Int? {
        val driftsavvik = persistenceService.hentDriftsavvik(driftsavvikId) ?: return null
        return persistenceService.lagreDriftsavvik(driftsavvik.copy(tidspunktTil = tidspunktTil))
    }
}
