package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.fil.avstemning.AvstemningsfilGenerator
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AvstemningService(
  private val avstemningsfilGenerator: AvstemningsfilGenerator,
  private val persistenceService: PersistenceService
) {

  fun startAvstemning(dato: LocalDate) {
    val konteringer = persistenceService.hentAlleKonteringerForDato(dato)
    avstemningsfilGenerator.skrivAvstemningfil(konteringer, dato)
  }
}