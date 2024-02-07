package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.fil.avstemning.AvstemmingsfilGenerator
import no.nav.bidrag.regnskap.fil.overføring.FiloverføringTilElinKlient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class AvstemmingService(
    private val avstemmingsfilGenerator: AvstemmingsfilGenerator,
    private val persistenceService: PersistenceService,
    private val filoverføringTilElinKlient: FiloverføringTilElinKlient,
) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AvstemmingService::class.java)
    }

    fun startAvstemming(dato: LocalDate) {
        LOGGER.info("Starter avstemning for dato: $dato")
        avstemmingsfilGenerator.skrivAvstemmingsfil(
            persistenceService.hentAlleKonteringerForDato(dato).filter { it.behandlingsstatusOkTidspunkt != null },
            dato,
        )
    }

    fun startAvstemming(dato: LocalDate, fomTidspunkt: LocalDateTime, tomTidspunkt: LocalDateTime) {
        LOGGER.info("Starter avstemning for dato: $dato for konteringer mellom $fomTidspunkt og $tomTidspunkt")
        val konteringer = persistenceService.hentAlleKonteringerForDato(
            dato,
            fomTidspunkt,
            tomTidspunkt,
        ).filter { it.behandlingsstatusOkTidspunkt != null }
        avstemmingsfilGenerator.skrivAvstemmingsfil(konteringer, dato)
    }

    fun startManuellOverføringAvsteming(dato: LocalDate) {
        val nowFormattert = dato.format(DateTimeFormatter.ofPattern("yyMMdd")).toString()
        val avstemmingMappe = "avstemning/"
        val avstemmingKonteringFilnavn = "avstdet_D$nowFormattert.xml"
        val avstemmingSummeringFilnavn = "avstsum_D$nowFormattert.xml"

        filoverføringTilElinKlient.lastOppFilTilFilsluse(avstemmingMappe, avstemmingKonteringFilnavn)
        filoverføringTilElinKlient.lastOppFilTilFilsluse(avstemmingMappe, avstemmingSummeringFilnavn)
    }
}
