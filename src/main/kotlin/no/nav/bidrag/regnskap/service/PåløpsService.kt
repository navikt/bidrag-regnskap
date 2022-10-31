package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.påløpRequest
import no.nav.bidrag.regnskap.dto.påløpResponse
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import org.springframework.stereotype.Service

@Service
class PåløpsService(
  val persistenceService: PersistenceService,
) {

  fun hentPåløp(): List<påløpResponse> {
    val påløpListe = persistenceService.hentPåløp()

    val påløpResponseListe = mutableListOf<påløpResponse>()

    påløpListe.forEach { påløp ->
      påløpResponseListe.add(
        påløpResponse(
          påløpId = påløp.påløpId,
          kjoredato = påløp.kjøredato.toString(),
          fullfortTidspunkt = påløp.fullførtTidspunkt.toString(),
          forPeriode = påløp.forPeriode
        )
      )
    }

    return påløpResponseListe
  }

  fun lagrePåløp(påløpRequest: påløpRequest): Int {
    val påløp = Påløp(
      kjøredato = påløpRequest.kjoredato, forPeriode = påløpRequest.forPeriode.toString()
    )

    return persistenceService.lagrePåløp(påløp)!!
  }
}