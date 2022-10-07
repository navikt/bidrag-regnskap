package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.PalopRequest
import no.nav.bidrag.regnskap.dto.PalopResponse
import no.nav.bidrag.regnskap.persistence.entity.Palop
import org.springframework.stereotype.Service

@Service
class PalopService(
  val persistenceService: PersistenceService,
) {

  fun hentPalop(): List<PalopResponse> {
    val palopListe = persistenceService.hentPalop()

    val palopResponseListe = mutableListOf<PalopResponse>()

    palopListe.forEach { palop ->
      palopResponseListe.add(
        PalopResponse(
          palopId = palop.palopId,
          kjoredato = palop.kjoredato.toString(),
          fullfortTidspunkt = palop.fullfortTidspunkt.toString(),
          forPeriode = palop.forPeriode
        )
      )
    }

    return palopResponseListe
  }

  fun lagrePalop(palopRequest: PalopRequest): Int {
    val palop = Palop(
      kjoredato = palopRequest.kjoredato,
      forPeriode = palopRequest.forPeriode.toString()
    )

    return persistenceService.lagrePalop(palop)!!
  }
}