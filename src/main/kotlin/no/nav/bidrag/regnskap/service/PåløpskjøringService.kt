package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.persistence.entity.Påløp
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
@Transactional
class PåløpskjøringService(
  private val persistenceService: PersistenceService,
  private val konteringService: KonteringService,
) {

  fun startPåløpskjøring(): ResponseEntity<Int> {
    val påløp = persistenceService.hentIkkeKjørtePåløp().minByOrNull { it.forPeriode }
      ?: return ResponseEntity.status(HttpStatus.NO_CONTENT).build()

    startPåløpskjøring(påløp)

    return ResponseEntity.status(HttpStatus.CREATED).body(påløp.påløpId)
  }

  @Async
  fun startPåløpskjøring(påløp: Påløp) {
    //Opprett driftavvik for å sperre opprettelse oversendelse av nye konteringer

    opprettKonteringerForAlleAktiveOppdrag(påløp)

    //Hent alle ikke oversendte konteringer

    //Skriv alle ikke oversendte konteringer til fil og sett de som oversendte

    påløp.fullførtTidspunkt = LocalDateTime.now()
    persistenceService.lagrePåløp(påløp)
  }

  private fun opprettKonteringerForAlleAktiveOppdrag(påløp: Påløp) {
    val periode = LocalDate.parse(påløp.forPeriode + "-01")
    val oppdragsperioder = persistenceService.hentAlleOppdragsperioderSomErAktiveForPeriode(periode)

    val (utgåtteOppdragsperioder, løpendeOppdragsperioder) = oppdragsperioder.partition {
      it.periodeTil?.minusMonths(1)?.isBefore(periode) == true
    }

    utgåtteOppdragsperioder.forEach {
      it.aktivTil = it.periodeTil
      persistenceService.lagreOppdragsperiode(it)
    }

    konteringService.opprettLøpendeKonteringerPåOppdragsperioder(løpendeOppdragsperioder, påløp.forPeriode)
  }
}