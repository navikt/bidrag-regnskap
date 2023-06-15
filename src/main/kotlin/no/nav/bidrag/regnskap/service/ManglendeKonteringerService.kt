package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.util.KonteringUtils.vurderSøknadType
import no.nav.bidrag.regnskap.util.KonteringUtils.vurderType
import no.nav.bidrag.regnskap.util.PeriodeUtils.erFørsteDatoSammeSomEllerTidligereEnnAndreDato
import no.nav.bidrag.regnskap.util.PeriodeUtils.hentAllePerioderMellomDato
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

private val LOGGER = LoggerFactory.getLogger(ManglendeKonteringerService::class.java)

@Service
class ManglendeKonteringerService {

    @Transactional
    fun opprettManglendeKonteringerForOppdragsperiode(oppdragsperiode: Oppdragsperiode, påløpsPeriode: YearMonth) {
        val perioderMellomDato = hentAllePerioderMellomDato(oppdragsperiode.periodeFra, oppdragsperiode.periodeTil, påløpsPeriode)

        perioderMellomDato.forEachIndexed { periodeIndex, periode ->
            if (oppdragsperiode.konteringer.any { it.overføringsperiode == periode.toString() }) {
                LOGGER.debug("Kontering for periode: $periode i oppdragsperiode: ${oppdragsperiode.oppdragsperiodeId} er allerede opprettet.")
            } else if (harIkkePassertAktivTilDato(oppdragsperiode, periode)) {
                oppdragsperiode.konteringer = oppdragsperiode.konteringer.plus(
                    Kontering(
                        transaksjonskode = Transaksjonskode.hentTransaksjonskodeForType(oppdragsperiode.oppdrag!!.stønadType).name,
                        overføringsperiode = periode.toString(),
                        type = vurderType(oppdragsperiode, periode),
                        søknadType = vurderSøknadType(oppdragsperiode.vedtakType, oppdragsperiode.oppdrag.stønadType, periodeIndex),
                        oppdragsperiode = oppdragsperiode,
                        sendtIPåløpsperiode = påløpsPeriode.toString(),
                        vedtakId = oppdragsperiode.vedtakId
                    )
                )
            }
        }
    }

    private fun harIkkePassertAktivTilDato(oppdragsperiode: Oppdragsperiode, påløpsPeriode: YearMonth) =
        !erFørsteDatoSammeSomEllerTidligereEnnAndreDato(oppdragsperiode.aktivTil, LocalDate.of(påløpsPeriode.year, påløpsPeriode.month, 1))
}
