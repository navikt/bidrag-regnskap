package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.påløp.PåløpRequest
import no.nav.bidrag.regnskap.dto.påløp.PåløpResponse
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class PåløpsService(
    val persistenceService: PersistenceService,
) {

    fun hentPåløp(): List<PåløpResponse> {
        val påløpListe = persistenceService.hentPåløp()

        return påløpListe.map {
            PåløpResponse(
                påløpId = it.påløpId,
                kjoredato = it.kjøredato.toString(),
                startetTidspunkt = it.startetTidspunkt?.toString(),
                fullfortTidspunkt = it.fullførtTidspunkt?.toString(),
                forPeriode = it.forPeriode,
            )
        }.sortedByDescending { YearMonth.parse(it.forPeriode) }
    }

    fun lagrePåløp(påløpRequest: PåløpRequest): Int {
        val påløp = Påløp(
            kjøredato = påløpRequest.kjoredato,
            forPeriode = påløpRequest.forPeriode.toString(),
        )

        return persistenceService.lagrePåløp(påløp)
    }
}
