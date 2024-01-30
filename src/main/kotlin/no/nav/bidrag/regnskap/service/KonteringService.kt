package no.nav.bidrag.regnskap.service

import no.nav.bidrag.domene.enums.regnskap.Søknadstype
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.domene.enums.regnskap.Type
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.util.KonteringUtils.vurderSøknadType
import no.nav.bidrag.regnskap.util.KonteringUtils.vurderType
import no.nav.bidrag.regnskap.util.PeriodeUtils
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Service
class KonteringService {

    fun opprettNyeKonteringerPåOppdragsperiode(oppdragsperiode: Oppdragsperiode, hendelse: Hendelse, sisteOverførtePeriode: YearMonth) {
        val perioderForOppdragsperiode =
            hentAllePerioderForOppdragsperiodeForSisteOverførtePeriode(oppdragsperiode, sisteOverførtePeriode)
        val alleOppdragsperioderForOppdrag = oppdragsperiode.oppdrag?.oppdragsperioder ?: emptyList()
        val transaksjonskode = Transaksjonskode.hentTransaksjonskodeForType(hendelse.type).name
        val vedtakId = hendelse.vedtakId

        perioderForOppdragsperiode.forEachIndexed { indexPeriode, periode ->

            // Om det opprettes et opphør og det ikke finnes eksisterende korrigerte konteringer for perioden skal det ikke opprettes ny kontering.
            if (oppdragsperiode.opphørendeOppdragsperiode && oppdragsperiode.beløp == BigDecimal.ZERO && !finnesKorrigerendeKonteringerForMåned(
                    periode,
                    oppdragsperiode,
                )
            ) {
                return@forEachIndexed
            }

            oppdragsperiode.konteringer = oppdragsperiode.konteringer.plus(
                Kontering(
                    transaksjonskode = transaksjonskode,
                    overføringsperiode = periode.toString(),
                    type = vurderType(alleOppdragsperioderForOppdrag, periode),
                    søknadType = vurderSøknadType(hendelse, indexPeriode),
                    oppdragsperiode = oppdragsperiode,
                    vedtakId = vedtakId,
                ),
            )
        }
    }

    private fun finnesKorrigerendeKonteringerForMåned(periode: YearMonth, oppdragsperiode: Oppdragsperiode): Boolean {
        return oppdragsperiode.oppdrag!!.oppdragsperioder.flatMap { it.konteringer }.any {
            YearMonth.parse(it.overføringsperiode) == periode && Transaksjonskode.valueOf(it.transaksjonskode).korreksjonskode == null
        }
    }

    fun opprettKorreksjonskonteringer(oppdrag: Oppdrag, oppdragsperiode: Oppdragsperiode, sisteOverførtePeriode: YearMonth, hendelse: Hendelse) {
        val overførteKonteringerListe = hentAlleKonteringerForOppdrag(oppdrag)

        opprettKorreksjonskonteringForAlleredeOversendteKonteringer(
            PeriodeUtils.hentAllePerioderMellomDato(oppdragsperiode.periodeFra, oppdragsperiode.periodeTil, sisteOverførtePeriode),
            overførteKonteringerListe,
            hendelse,
        )
    }

    private fun opprettKorreksjonskonteringForAlleredeOversendteKonteringer(
        perioderForOppdragsperiode: List<YearMonth>,
        overførteKonteringerListe: List<Kontering>,
        hendelse: Hendelse,
    ) {
        overførteKonteringerListe.forEach { kontering ->
            val korreksjonskode = Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode

            if (korreksjonskode != null && !erOverførtKonteringAlleredeKorrigert(kontering, overførteKonteringerListe) &&
                erPeriodeOverlappendeSlutterFørOverførteKonteringsperiodeEllerGebyr(perioderForOppdragsperiode, kontering)
            ) {
                val nyKorreksjonskontering = Kontering(
                    oppdragsperiode = kontering.oppdragsperiode,
                    overføringsperiode = kontering.overføringsperiode,
                    transaksjonskode = korreksjonskode,
                    type = Type.ENDRING.name,
                    søknadType = kontering.søknadType,
                    vedtakId = hendelse.vedtakId,
                )

                kontering.oppdragsperiode?.konteringer = kontering.oppdragsperiode?.konteringer?.plus(nyKorreksjonskontering)
                    ?: error("Noe gikk galt ved opprettelse av korrekterende kontering for konteringId: ${kontering.konteringId}")
            }
        }
    }

    private fun erPeriodeOverlappendeSlutterFørOverførteKonteringsperiodeEllerGebyr(
        perioderForOppdragsperiode: List<YearMonth>,
        kontering: Kontering,
    ): Boolean {
        return erPeriodeOverlappende(perioderForOppdragsperiode, kontering) ||
            slutterNyeOppdragsperiodeFørOverførteKonteringsPeriode(kontering, perioderForOppdragsperiode) ||
            erKonteringGebyr(kontering)
    }

    private fun erKonteringGebyr(kontering: Kontering): Boolean {
        return (kontering.søknadType == Søknadstype.FABM.name || kontering.søknadType == Søknadstype.FABP.name)
    }

    private fun slutterNyeOppdragsperiodeFørOverførteKonteringsPeriode(kontering: Kontering, perioderForNyOppdrasperiode: List<YearMonth>): Boolean {
        val maxOppdragsperiode = perioderForNyOppdrasperiode.maxOrNull() ?: return false
        return YearMonth.parse(kontering.overføringsperiode).isAfter(maxOppdragsperiode)
    }

    private fun erPeriodeOverlappende(perioderForNyOppdrasperiode: List<YearMonth>, kontering: Kontering): Boolean {
        return perioderForNyOppdrasperiode.contains(YearMonth.parse(kontering.overføringsperiode))
    }

    private fun hentAllePerioderForOppdragsperiodeForSisteOverførtePeriode(
        oppdragsperiode: Oppdragsperiode,
        sisteOverførtePeriode: YearMonth,
    ): List<YearMonth> {
        return PeriodeUtils.hentAllePerioderMellomDato(
            oppdragsperiode.periodeFra,
            oppdragsperiode.periodeTil,
            sisteOverførtePeriode,
        ).filter { it.isBefore(sisteOverførtePeriode.plusMonths(1)) }
    }

    private fun erOverførtKonteringAlleredeKorrigert(kontering: Kontering, overførteKonteringerListe: List<Kontering>): Boolean {
        return overførteKonteringerListe.any {
            it.transaksjonskode == Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode &&
                it.oppdragsperiode == kontering.oppdragsperiode &&
                it.overføringsperiode == kontering.overføringsperiode
        }
    }

    fun hentAlleKonteringerForOppdrag(oppdrag: Oppdrag): List<Kontering> {
        return oppdrag.oppdragsperioder.flatMap { it.konteringer }
    }

    fun opprettManglendeKonteringerVedOppstartAvOpphørtOppdragsperiode(
        oppdrag: Oppdrag,
        oppdragsperiode: Oppdragsperiode,
        sisteOverførtePeriode: YearMonth,
        hendelse: Hendelse,
    ) {
        oppdrag.oppdragsperioder
            .filter { it.aktivTil == null && it.opphørendeOppdragsperiode }
            .forEach {
                val kontering = it.konteringer.maxByOrNull { kontering -> kontering.overføringsperiode } ?: return
                val nestePeriode = YearMonth.parse(kontering.overføringsperiode).plusMonths(1)
                val perioder = PeriodeUtils.hentAllePerioderMellomDato(
                    LocalDate.of(nestePeriode.year, nestePeriode.month, 1),
                    oppdragsperiode.periodeFra,
                    sisteOverførtePeriode,
                )
                perioder.forEach { periode ->
                    it.konteringer = it.konteringer.plus(
                        Kontering(
                            oppdragsperiode = kontering.oppdragsperiode,
                            overføringsperiode = periode.toString(),
                            transaksjonskode = kontering.transaksjonskode,
                            type = Type.NY.name,
                            søknadType = kontering.søknadType,
                            vedtakId = hendelse.vedtakId,
                        ),
                    )
                }
            }
    }
}
