package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.fil.avstemning.AvstemmingsfilGenerator
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AvstemmingService(
    private val avstemmingsfilGenerator: AvstemmingsfilGenerator,
    private val persistenceService: PersistenceService
) {

    fun startAvstemming(dato: LocalDate) {
        val konteringer = persistenceService.hentAlleKonteringerForDato(dato)
        avstemmingsfilGenerator.skrivAvstemmingsfil(konteringer, dato)
    }
}
