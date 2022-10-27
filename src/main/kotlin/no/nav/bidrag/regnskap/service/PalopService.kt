package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.palopRequest
import no.nav.bidrag.regnskap.dto.palopResponse
import no.nav.bidrag.regnskap.persistence.entity.Palop
import org.springframework.stereotype.Service

@Service
class PalopService(
  val persistenceService: PersistenceService,
) {

  fun hentPalop(): List<palopResponse> {
    val palopListe = persistenceService.hentPalop()

    val palopResponseListe = mutableListOf<palopResponse>()

    palopListe.forEach { palop ->
      palopResponseListe.add(
        palopResponse(
          palopId = palop.palopId,
          kjoredato = palop.kjoredato.toString(),
          fullfortTidspunkt = palop.fullfortTidspunkt.toString(),
          forPeriode = palop.forPeriode
        )
      )
    }

    return palopResponseListe
  }

  fun lagrePalop(palopRequest: palopRequest): Int {
    val palop = Palop(
      kjoredato = palopRequest.kjoredato, forPeriode = palopRequest.forPeriode.toString()
    )

    return persistenceService.lagrePalop(palop)!!
  }
}