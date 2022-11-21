package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.påløp.PåløpRequest
import no.nav.bidrag.regnskap.dto.påløp.PåløpResponse
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import org.springframework.stereotype.Service

@Service
class PåløpsService(
  val persistenceService: PersistenceService,
) {

  fun hentPåløp(): List<PåløpResponse> {
    val påløpListe = persistenceService.hentPåløp()

    val påløpResponseListe = mutableListOf<PåløpResponse>()

    påløpListe.forEach { påløp ->
      påløpResponseListe.add(
        PåløpResponse(
          påløpId = påløp.påløpId,
          kjoredato = påløp.kjøredato.toString(),
          fullfortTidspunkt = påløp.fullførtTidspunkt.toString(),
          forPeriode = påløp.forPeriode
        )
      )
    }

    return påløpResponseListe
  }

  fun lagrePåløp(påløpRequest: PåløpRequest): Int {
    val påløp = Påløp(
      kjøredato = påløpRequest.kjoredato, forPeriode = påløpRequest.forPeriode.toString()
    )

    return persistenceService.lagrePåløp(påløp)!!
  }
}