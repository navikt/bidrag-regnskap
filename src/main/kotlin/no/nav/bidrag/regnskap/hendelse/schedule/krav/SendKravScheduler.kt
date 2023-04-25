package no.nav.bidrag.regnskap.hendelse.schedule.krav

import io.github.oshai.KotlinLogging
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.regnskap.service.KravService
import no.nav.bidrag.regnskap.service.PersistenceService
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger { }

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class SendKravScheduler(
    private val persistenceService: PersistenceService,
    private val kravService: KravService,
    private val kravSchedulerUtils: KravSchedulerUtils
) {

    @Scheduled(cron = "\${scheduler.sendkrav.cron}")
    @SchedulerLock(name = "skedulertOverforingAvKrav")
    @Transactional
    fun skedulertOverforingAvKrav() {
        LockAssert.assertLocked()
        LOGGER.info { "Starter schedulert overføring av alle konteringer som ikke har blitt overført." }
        if (kravSchedulerUtils.harAktiveDriftAvvik()) {
            LOGGER.info { "Det finnes aktive driftsavvik. Starter derfor ikke overføring av krav." }
            return
        } else if (kravSchedulerUtils.erVedlikeholdsmodusPåslått()) {
            LOGGER.info { "Vedlikeholdsmodus er påslått! Starter derfor ikke overføring av krav." }
            return
        }

        val oppdragMedIkkeOverforteKonteringer = hentOppdragMedIkkeOverforteKonteringerHvorKonteringIkkeErUtsatt()

        if (oppdragMedIkkeOverforteKonteringer.isEmpty()) {
            LOGGER.info { "Det finnes ingen oppdrag med unsendte konteringer som ikke skal utsettes." }
            return
        }

        oppdragMedIkkeOverforteKonteringer.forEach {
            kravService.sendKrav(it)
        }
        LOGGER.info("Alle oppdrag(antall: ${oppdragMedIkkeOverforteKonteringer.size}) med unsendte konteringer er nå overført til skatt.")
    }

    private fun hentOppdragMedIkkeOverforteKonteringerHvorKonteringIkkeErUtsatt(): List<Int> {
        return persistenceService.hentAlleIkkeOverførteKonteringer()
            .flatMap { listOf(it.oppdragsperiode?.oppdrag) }
            .filterNot { it?.utsattTilDato?.isAfter(LocalDate.now()) == true }
            .map { it?.oppdragId }
            .distinct()
            .filterNotNull()
    }
}
