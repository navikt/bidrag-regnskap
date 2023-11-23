package no.nav.bidrag.regnskap.hendelse.schedule.krav

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.service.KravService
import no.nav.bidrag.regnskap.service.PersistenceService
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger { }

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class SendKravScheduler(
    private val persistenceService: PersistenceService,
    private val kravService: KravService,
    private val kravSchedulerUtils: KravSchedulerUtils,
) {

    companion object {
        private const val KONTERING_MINIMUM_LEVETID = 30L
    }

    @Scheduled(cron = "\${scheduler.sendkrav.cron}")
    @SchedulerLock(name = "skedulertOverforingAvKrav")
    @Transactional
    fun skedulertOverforingAvKrav() {
        LockAssert.assertLocked()
        LOGGER.info { "Starter schedulert overføring av alle konteringer som ikke har blitt overført." }
        if (kravSchedulerUtils.harAktivtDriftsavvik()) {
            LOGGER.info { "Det finnes aktive driftsavvik. Starter derfor ikke overføring av krav." }
            return
        } else if (kravSchedulerUtils.erVedlikeholdsmodusPåslått()) {
            LOGGER.info { "Vedlikeholdsmodus er påslått! Starter derfor ikke overføring av krav." }
            return
        }

        val oppdragMedIkkeOverførteKonteringer = hentOppdragMedIkkeOverførteKonteringerHvorKonteringIkkeErUtsatt()

        if (oppdragMedIkkeOverførteKonteringer.isEmpty()) {
            LOGGER.info { "Det finnes ingen oppdrag med unsendte konteringer som ikke skal utsettes." }
            return
        }

        // Samler alle oppdrag for samme sak slik at oversending til ELIN mottar de i samme krav.
        val sakerMedIkkeOverførteKonteringer = HashMap<String, MutableList<Int>>()
        oppdragMedIkkeOverførteKonteringer.forEach {
            if (!oppdragHarNyligOpprettedeKonteringer(it)) {
                sakerMedIkkeOverførteKonteringer.getOrPut(it.sakId) { mutableListOf() }.apply { add(it.oppdragId) }
            }
        }

        sakerMedIkkeOverførteKonteringer.forEach {
            kravService.sendKrav(it.value)
        }

        LOGGER.info { "Alle oppdrag(antall: ${oppdragMedIkkeOverførteKonteringer.size}) med unsendte konteringer er nå forsøkt overført til skatt." }
    }

    private fun oppdragHarNyligOpprettedeKonteringer(oppdrag: Oppdrag): Boolean {
        return oppdrag.oppdragsperioder.any { oppdragsperiode ->
            oppdragsperiode.konteringer.any { kontering ->
                kontering.opprettetTidspunkt.isAfter(LocalDateTime.now().minusSeconds(KONTERING_MINIMUM_LEVETID))
            }
        }
    }

    private fun hentOppdragMedIkkeOverførteKonteringerHvorKonteringIkkeErUtsatt(): List<Oppdrag> {
        return persistenceService.hentAlleIkkeOverførteKonteringer()
            .flatMap { listOf(it.oppdragsperiode?.oppdrag) }
            .filterNot { it?.utsattTilDato?.isAfter(LocalDate.now()) == true }
            .filterNotNull()
            .distinct()
    }
}
