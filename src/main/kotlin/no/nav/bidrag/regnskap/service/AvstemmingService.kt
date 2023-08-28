package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.fil.avstemning.AvstemmingsfilGenerator
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class AvstemmingService(
    private val avstemmingsfilGenerator: AvstemmingsfilGenerator,
    private val persistenceService: PersistenceService
) {

    fun startAvstemming(dato: LocalDate) {
        avstemmingsfilGenerator.skrivAvstemmingsfil(
            persistenceService.hentAlleKonteringerForDato(dato).filter { it.behandlingsstatusOkTidspunkt != null },
            dato
        )
    }

    fun startAvstemming(dato: LocalDate, fomTidspunkt: LocalDateTime, tomTidspunkt: LocalDateTime) {
        val konteringer = persistenceService.hentAlleKonteringerForDato(dato, fomTidspunkt, tomTidspunkt).filter { it.behandlingsstatusOkTidspunkt != null }
        avstemmingsfilGenerator.skrivAvstemmingsfil(konteringer, dato)
    }
}
