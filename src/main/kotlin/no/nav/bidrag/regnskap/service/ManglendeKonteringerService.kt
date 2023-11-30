package no.nav.bidrag.regnskap.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
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
import java.time.YearMonth

private val LOGGER = KotlinLogging.logger { }

@Service
class ManglendeKonteringerService(
    private val oppdragsperiodeRepo: OppdragsperiodeRepository,
    private val persistenceService: PersistenceService,
    @Value("\${KONTERINGER_FORELDET_DATO}") private val konteringerForeldetDato: String,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettKonteringerForAlleOppdragsperiodePartisjon(påløpsPeriode: LocalDate, oppdragsperiodeIds: List<Int>) {
        val startTime = System.currentTimeMillis()
        oppdragsperiodeIds.forEach {
            opprettKonteringerForOppdragsperiode(påløpsPeriode, it)
        }
        LOGGER.debug {
            "TIDSBRUK opprettKonteringerForAlleOppdragsperiodePartisjon: ${System.currentTimeMillis() - startTime}ms, Ledig minne: ${
                Runtime.getRuntime().freeMemory()
            }"
        }
    }

    @Transactional
    fun opprettKonteringerForOppdragsperiode(påløpsPeriode: LocalDate, oppdragsperiodeId: Int) {
        LOGGER.info { "Oppretter konteringer for oppdragsperiode $oppdragsperiodeId" }
        val startTime = System.currentTimeMillis()
        val oppdragsperiode = oppdragsperiodeRepo.findById(oppdragsperiodeId).get()

        if (oppdragsperiode.aktivTil == null && erFørsteDatoSammeSomEllerTidligereEnnAndreDato(oppdragsperiode.periodeTil, påløpsPeriode)) {
            oppdragsperiode.aktivTil = oppdragsperiode.periodeTil
        }

        opprettManglendeKonteringerForOppdragsperiode(
            oppdragsperiode,
            YearMonth.from(påløpsPeriode),
        )

        if (erFørsteDatoSammeSomEllerTidligereEnnAndreDato(oppdragsperiode.aktivTil, påløpsPeriode)) {
            oppdragsperiode.konteringerFullførtOpprettet = true
        }

        persistenceService.lagreOppdragsperiode(oppdragsperiode)
        LOGGER.debug { "TIDSBRUK opprettKonteringerForAlleOppdragsperiode: ${System.currentTimeMillis() - startTime}ms" }
    }

    @Transactional
    fun opprettManglendeKonteringerForOppdragsperiode(oppdragsperiode: Oppdragsperiode, påløpsPeriode: YearMonth) {
        val startTime = System.currentTimeMillis()
        val perioderMellomDato = hentAllePerioderMellomDato(oppdragsperiode.periodeFra, oppdragsperiode.periodeTil, påløpsPeriode)

        perioderMellomDato
            .filterNot { it.isBefore(YearMonth.parse(konteringerForeldetDato)) }
            .forEachIndexed { periodeIndex, periode ->
                if (oppdragsperiode.konteringer.any { it.overføringsperiode == periode.toString() }) {
                    LOGGER.debug { "Kontering for periode: $periode i oppdragsperiode: ${oppdragsperiode.oppdragsperiodeId} er allerede opprettet." }
                } else if (harIkkePassertAktivTilDato(oppdragsperiode, periode)) {
                    oppdragsperiode.konteringer = oppdragsperiode.konteringer.plus(
                        Kontering(
                            transaksjonskode = Transaksjonskode.hentTransaksjonskodeForType(oppdragsperiode.oppdrag!!.stønadType).name,
                            overføringsperiode = periode.toString(),
                            type = vurderType(oppdragsperiode, periode),
                            søknadType = vurderSøknadType(oppdragsperiode.vedtakType, oppdragsperiode.oppdrag.stønadType, periodeIndex),
                            oppdragsperiode = oppdragsperiode,
                            sendtIPåløpsperiode = påløpsPeriode.toString(),
                            vedtakId = oppdragsperiode.vedtakId,
                        ),
                    )
                }
            }
        LOGGER.debug { "TIDSBRUK opprettManglendeKonteringerForOppdragsperiode: ${System.currentTimeMillis() - startTime}ms" }
    }

    private fun harIkkePassertAktivTilDato(oppdragsperiode: Oppdragsperiode, periode: YearMonth) =
        !erFørsteDatoSammeSomEllerTidligereEnnAndreDato(oppdragsperiode.aktivTil, LocalDate.of(periode.year, periode.month, 1))
}
