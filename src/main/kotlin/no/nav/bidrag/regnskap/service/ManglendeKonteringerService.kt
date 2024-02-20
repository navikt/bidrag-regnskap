package no.nav.bidrag.regnskap.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import no.nav.bidrag.regnskap.util.KonteringUtils.vurderSøknadType
import no.nav.bidrag.regnskap.util.KonteringUtils.vurderType
import no.nav.bidrag.regnskap.util.PeriodeUtils.erFørsteDatoSammeSomEllerTidligereEnnAndreDato
import no.nav.bidrag.regnskap.util.PeriodeUtils.hentAllePerioderMellomDato
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private val LOGGER = KotlinLogging.logger { }

@Service
class ManglendeKonteringerService(
    private val oppdragsperiodeRepo: OppdragsperiodeRepository,
    private val persistenceService: PersistenceService,
    @Value("\${KONTERINGER_FORELDET_DATO}") private val konteringerForeldetDato: String,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettKonteringerForOppdragsperiode(påløp: Påløp, oppdragsperiodeIder: List<Int>): List<Int> {
        val timestamp = LocalDateTime.now()
        val påløpsPeriode = LocalDate.parse(påløp.forPeriode + "-01")
        val påløpKjøredato = påløp.kjøredato.toLocalDate()

        val oppdragsperioder = oppdragsperiodeRepo.hentAlleOppdragsperioderForListe(oppdragsperiodeIder)

        // Tar ut utsatte eller feilede oppdragsperioder slik at det ikke opprettes
        val (utsatteEllerFeiledeOppdragsperioder, oppdragsperioderSomSkalBehandles) = oppdragsperioder.partition { oppdragsperiode ->
            oppdragsperiode.oppdrag?.utsattTilDato?.isAfter(påløpKjøredato) == true ||
                oppdragsperiode.konteringer.any {
                    it.overføringstidspunkt != null && it.behandlingsstatusOkTidspunkt == null
                }
        }

        oppdragsperioderSomSkalBehandles.forEach { oppdragsperiode ->

            if (oppdragsperiode.aktivTil == null && erFørsteDatoSammeSomEllerTidligereEnnAndreDato(oppdragsperiode.periodeTil, påløpsPeriode)) {
                oppdragsperiode.aktivTil = oppdragsperiode.periodeTil
            }

            opprettManglendeKonteringerForOppdragsperiode(oppdragsperiode, YearMonth.from(påløpsPeriode), timestamp)

            if (erFørsteDatoSammeSomEllerTidligereEnnAndreDato(oppdragsperiode.aktivTil, påløpsPeriode)) {
                oppdragsperiode.konteringerFullførtOpprettet = true
            }
        }

        persistenceService.lagreOppdragsperioder(oppdragsperioder)

        return utsatteEllerFeiledeOppdragsperioder.map { it.oppdragsperiodeId }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettKonteringerForUtsatteOgFeiledeOppdragsperiode(påløp: Påløp, oppdragsperiodeIder: List<Int>) {
        val påløpsPeriode = LocalDate.parse(påløp.forPeriode + "-01")

        val oppdragsperioder = oppdragsperiodeRepo.hentAlleOppdragsperioderForListe(oppdragsperiodeIder)

        oppdragsperioder.forEach { oppdragsperiode ->

            if (oppdragsperiode.aktivTil == null && erFørsteDatoSammeSomEllerTidligereEnnAndreDato(oppdragsperiode.periodeTil, påløpsPeriode)) {
                oppdragsperiode.aktivTil = oppdragsperiode.periodeTil
            }

            opprettManglendeKonteringerForOppdragsperiode(
                oppdragsperiode,
                YearMonth.from(påløpsPeriode),
                null,
                settPåløpsperiode = false,
                settOverføringstidspunkt = false,
                settBehandlingsstatusOkTidspunkt = false,
            )

            if (erFørsteDatoSammeSomEllerTidligereEnnAndreDato(oppdragsperiode.aktivTil, påløpsPeriode)) {
                oppdragsperiode.konteringerFullførtOpprettet = true
            }
        }

        persistenceService.lagreOppdragsperioder(oppdragsperioder)
    }

    fun opprettManglendeKonteringerForOppdragsperiode(
        oppdragsperiode: Oppdragsperiode,
        påløpsPeriode: YearMonth,
        timestamp: LocalDateTime?,
        settPåløpsperiode: Boolean = true,
        settOverføringstidspunkt: Boolean = true,
        settBehandlingsstatusOkTidspunkt: Boolean = true,
    ) {
        val perioderMellomDato = hentAllePerioderMellomDato(oppdragsperiode.periodeFra, oppdragsperiode.periodeTil, påløpsPeriode)
        val konteringerForelderYearMonth = YearMonth.parse(konteringerForeldetDato)
        val transaksjonskode = Transaksjonskode.hentTransaksjonskodeForType(oppdragsperiode.oppdrag!!.stønadType).name
        val stønadType = oppdragsperiode.oppdrag.stønadType
        val vedtakType = oppdragsperiode.vedtakType
        val vedtakId = oppdragsperiode.vedtakId
        val alleOppdragsperioderPåOppdrag = oppdragsperiode.oppdrag.oppdragsperioder

        perioderMellomDato.filterNot { it.isBefore(konteringerForelderYearMonth) }.forEachIndexed { periodeIndex, periode ->
            if (oppdragsperiode.konteringer.any { it.overføringsperiode == periode.toString() }) {
                LOGGER.debug { "Kontering for periode: $periode i oppdragsperiode: ${oppdragsperiode.oppdragsperiodeId} er allerede opprettet." }
            } else if (harIkkePassertAktivTilDato(oppdragsperiode, periode)) {
                oppdragsperiode.konteringer = oppdragsperiode.konteringer.plus(
                    Kontering(
                        transaksjonskode = transaksjonskode,
                        overføringsperiode = periode.toString(),
                        type = vurderType(alleOppdragsperioderPåOppdrag, periode),
                        søknadType = vurderSøknadType(vedtakType, stønadType, periodeIndex),
                        oppdragsperiode = oppdragsperiode,
                        sendtIPåløpsperiode = if (settPåløpsperiode) påløpsPeriode.toString() else null,
                        vedtakId = vedtakId,
                        overføringstidspunkt = if (settOverføringstidspunkt) timestamp else null,
                        behandlingsstatusOkTidspunkt = if (settBehandlingsstatusOkTidspunkt) timestamp else null,
                    ),
                )
            }
        }
    }

    private fun harIkkePassertAktivTilDato(oppdragsperiode: Oppdragsperiode, periode: YearMonth): Boolean {
        return !erFørsteDatoSammeSomEllerTidligereEnnAndreDato(oppdragsperiode.aktivTil, LocalDate.of(periode.year, periode.month, 1))
    }
}
