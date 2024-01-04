package no.nav.bidrag.regnskap.hendelse.schedule.krav

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.regnskap.service.PersistenceService
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger { }

/**
 * Denne scheduled tasken er opprettet for å sørge for at feilede krav forsøkes å sendes over på nytt automatisk.
 * Dette gjøres ved å sette overføringstidspunkt til null, slik at SendKravScheduler plukker opp kravene ved neste kjøring.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class ResendingAvKravScheduler(
    private val persistenceService: PersistenceService,
) {

    @Scheduled(cron = "\${scheduler.resendkrav.cron}")
    @SchedulerLock(name = "skedulertResendingAvKrav")
    @Transactional
    fun skedulertResendingAvKrav() {
        LockAssert.assertLocked()
        LOGGER.info { "Starter schedulert resending av alle krav som ikke har fått behandlingsstatus ok." }

        val konteringerSomIkkeHarFåttGodkjentBehandlingsstatus = persistenceService.hentAlleKonteringerUtenBehandlingsstatusOk()

        if (konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.isEmpty()) {
            LOGGER.info { "Det finnes ingen konteringer som ikke har sjekket behandlingsstatus." }
            return
        }

        konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.forEach {
            it.overføringstidspunkt = null
        }

        persistenceService.konteringRepository.saveAll(konteringerSomIkkeHarFåttGodkjentBehandlingsstatus)
    }
}
