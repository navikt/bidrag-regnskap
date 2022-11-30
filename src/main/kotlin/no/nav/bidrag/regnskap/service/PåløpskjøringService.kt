package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.dto.enumer.ÅrsakKode
import no.nav.bidrag.regnskap.dto.påløp.Vedlikeholdsmodus
import no.nav.bidrag.regnskap.persistence.entity.Driftsavvik
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.OverføringKontering
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.bidrag.regnskap.fil.PåløpsfilGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import javax.transaction.Transactional

private val LOGGER = LoggerFactory.getLogger(PåløpskjøringService::class.java)

@Service
class PåløpskjøringService(
  private val persistenceService: PersistenceService,
  private val konteringService: KonteringService,
  private val påløpsfilGenerator: PåløpsfilGenerator,
  private val skattConsumer: SkattConsumer
) {

  fun startPåløpskjøring(påløp: Påløp, schedulertKjøring: Boolean) {
    validerDriftsavvik(påløp, schedulertKjøring)
    endreElinVedlikeholdsmodus(ÅrsakKode.PAALOEP_GENERERES, "Påløp for ${påløp.forPeriode} genereres hos NAV.")
    opprettKonteringerForAlleAktiveOppdrag(påløp)
    genererPåløpsfil(påløp)
    fullførPåløp(påløp)
    endreElinVedlikeholdsmodus(ÅrsakKode.PAALOEP_LEVERT, "Påløp for ${påløp.forPeriode} er ferdig generert fra NAV.")
    avsluttDriftsavvik(påløp)
  }

  @Transactional
  fun hentPåløp() = persistenceService.hentIkkeKjørtePåløp().minByOrNull { it.forPeriode }

  @Transactional
  fun genererPåløpsfil(påløp: Påløp) {
    LOGGER.info("Starter generering av påløpsfil...")
    val konteringer = persistenceService.hentAlleIkkeOverførteKonteringer()
    påløpsfilGenerator.skrivPåløpsfilOgLastOppPåFilsluse(konteringer, påløp)
    settKonteringTilOverførtOgOpprettOverføringKontering(konteringer, påløp)
    LOGGER.info("Påløpsfil er ferdig skrevet med ${konteringer.size} konteringer og lastet opp til filsluse.")
  }

  private fun settKonteringTilOverførtOgOpprettOverføringKontering(konteringer: List<Kontering>, påløp: Påløp) {
    val timestamp = LocalDateTime.now()
    konteringer.forEach {
      it.overføringstidspunkt = timestamp
      it.oppdragsperiode?.oppdrag?.sistOversendtePeriode = påløp.forPeriode

      persistenceService.lagreKontering(it)

      persistenceService.lagreOverføringKontering(
        OverføringKontering(
          kontering = it, tidspunkt = timestamp, kanal = "Påløpsfil"
        )
      )
    }
  }

  @Transactional
  fun fullførPåløp(påløp: Påløp) {
    påløp.fullførtTidspunkt = LocalDateTime.now()
    persistenceService.lagrePåløp(påløp)
  }

  @Transactional
  fun avsluttDriftsavvik(påløp: Påløp) {
    val driftsavvik = persistenceService.hentDriftsavvikForPåløp(påløp.påløpId!!)!!
    driftsavvik.tidspunktTil = LocalDateTime.now()
    persistenceService.lagreDriftsavvik(driftsavvik)
  }

  @Transactional
  fun validerDriftsavvik(påløp: Påløp, schedulertKjøring: Boolean) {
    val driftsavvikListe = persistenceService.hentAlleAktiveDriftsavvik()
    if (driftsavvikListe.any { it.påløpId != påløp.påløpId }) {
      LOGGER.error("Det finnes aktive driftsavvik som ikke er knyttet til påløpet! Kan derfor ikke starte påløpskjøring!")
      //Todo() Bedre feilhåndtering
      throw IllegalStateException("Det finnes aktive driftsavvik som ikke er knyttet til påløpet! Kan derfor ikke starte påløpskjøring!")
    }
    if (driftsavvikListe.isEmpty()) persistenceService.lagreDriftsavvik(
      opprettDriftsavvik(
        påløp, schedulertKjøring
      )
    )
  }

  private fun opprettDriftsavvik(
    påløp: Påløp, schedulertKjøring: Boolean
  ) = Driftsavvik(
    påløpId = påløp.påløpId,
    tidspunktFra = LocalDateTime.now(),
    opprettetAv = if (schedulertKjøring) "Automatisk påløpskjøringer" else "Manuel påløpskjøring (REST)",
    årsak = "Påløpskjøring"
  )

  @Transactional
  fun opprettKonteringerForAlleAktiveOppdrag(påløp: Påløp) {
    val periode = LocalDate.parse(påløp.forPeriode + "-01")
    val oppdragsperioder = persistenceService.hentAlleOppdragsperioderSomErAktiveForPeriode(periode)

    val (utgåtteOppdragsperioder, løpendeOppdragsperioder) = oppdragsperioder.partition {
      it.periodeTil?.minusMonths(1)?.isBefore(periode) == true
    }

    utgåtteOppdragsperioder.forEach {
      it.aktivTil = it.periodeTil
      persistenceService.lagreOppdragsperiode(it)
    }

    konteringService.opprettLøpendeKonteringerPåOppdragsperioder(
      løpendeOppdragsperioder.filter {
        periode.plusMonths(1).isAfter(it.periodeFra)
      }, påløp.forPeriode
    )
  }

  private fun endreElinVedlikeholdsmodus(årsakKode: ÅrsakKode, kommentar: String) {
    skattConsumer.oppdaterVedlikeholdsmodus(Vedlikeholdsmodus(true, årsakKode, kommentar))
  }
}