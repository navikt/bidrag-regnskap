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
        avstemmingsfilGenerator.skrivAvstemmingsfil(
            persistenceService.hentAlleKonteringerForDato(dato).filter { it.behandlingsstatusOkTidspunkt != null },
            dato
        )
    }
}
