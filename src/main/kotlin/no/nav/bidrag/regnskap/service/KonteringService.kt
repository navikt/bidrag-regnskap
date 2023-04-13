package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.enumer.Søknadstype
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.dto.enumer.Type
import no.nav.bidrag.regnskap.dto.oppdrag.KonteringResponse
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.util.KonteringUtils.vurderSøknadType
import no.nav.bidrag.regnskap.util.KonteringUtils.vurderType
import no.nav.bidrag.regnskap.util.PeriodeUtils
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class KonteringService {

    fun hentKonteringer(oppdrag: Oppdrag): List<KonteringResponse> {
        val konteringResponser = mutableListOf<KonteringResponse>()

        oppdrag.oppdragsperioder.forEach { oppdragsperiode ->
            oppdragsperiode.konteringer.forEach { kontering ->
                konteringResponser.add(
                    KonteringResponse(
                        konteringId = kontering.konteringId,
                        oppdragsperiodeId = kontering.oppdragsperiode?.oppdragsperiodeId,
                        transaksjonskode = Transaksjonskode.valueOf(kontering.transaksjonskode),
                        overforingsperiode = kontering.overføringsperiode,
                        overforingstidspunkt = kontering.overføringstidspunkt,
                        type = Type.valueOf(kontering.type),
                        soknadType = Søknadstype.valueOf(kontering.søknadType),
                        sendtIPalopsfil = kontering.sendtIPåløpsfil
                    )
                )
            }
        }
        return konteringResponser
    }

    fun opprettNyeKonteringerPåOppdragsperiode(
        oppdragsperiode: Oppdragsperiode,
        hendelse: Hendelse,
        sisteOverførtePeriode: YearMonth
    ) {
        val perioderForOppdragsperiode =
            hentAllePerioderForOppdragsperiodeForSisteOverførtePeriode(oppdragsperiode, sisteOverførtePeriode)

        perioderForOppdragsperiode.forEachIndexed { indexPeriode, periode ->
            oppdragsperiode.konteringer = oppdragsperiode.konteringer.plus(
                Kontering(
                    transaksjonskode = Transaksjonskode.hentTransaksjonskodeForType(hendelse.type).name,
                    overføringsperiode = periode.toString(),
                    type = vurderType(oppdragsperiode, periode),
                    søknadType = vurderSøknadType(hendelse, indexPeriode),
                    oppdragsperiode = oppdragsperiode
                )
            )
        }
    }

    fun opprettKorreksjonskonteringer(oppdrag: Oppdrag, oppdragsperiode: Oppdragsperiode, sisteOverførtePeriode: YearMonth) {
        val overførteKonteringerListe = hentAlleKonteringerForOppdrag(oppdrag)

        opprettKorreksjonskonteringForAlleredeOversendteKonteringer(
            PeriodeUtils.hentAllePerioderMellomDato(oppdragsperiode.periodeFra, oppdragsperiode.periodeTil, sisteOverførtePeriode),
            overførteKonteringerListe
        )
    }

    private fun opprettKorreksjonskonteringForAlleredeOversendteKonteringer(
        perioderForOppdragsperiode: List<YearMonth>,
        overførteKonteringerListe: List<Kontering>
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
                    type = Type.ENDRING.toString(),
                    søknadType = kontering.søknadType
                )

                kontering.oppdragsperiode?.konteringer = kontering.oppdragsperiode?.konteringer?.plus(nyKorreksjonskontering)
                    ?: error("Noe gikk galt ved opprettelse av korrekterende kontering for konteringId: ${kontering.konteringId}")
            }
        }
    }

    private fun erPeriodeOverlappendeSlutterFørOverførteKonteringsperiodeEllerGebyr(
        perioderForOppdragsperiode: List<YearMonth>,
        kontering: Kontering
    ): Boolean {
        return erPeriodeOverlappende(perioderForOppdragsperiode, kontering) ||
            slutterNyeOppdragsperiodeFørOverførteKonteringsPeriode(kontering, perioderForOppdragsperiode) ||
            erKonteringGebyr(kontering)
    }

    private fun erKonteringGebyr(kontering: Kontering): Boolean {
        return (kontering.søknadType == Søknadstype.FABM.name || kontering.søknadType == Søknadstype.FABP.name)
    }

    private fun slutterNyeOppdragsperiodeFørOverførteKonteringsPeriode(
        kontering: Kontering,
        perioderForNyOppdrasperiode: List<YearMonth>
    ): Boolean {
        val maxOppdragsperiode = perioderForNyOppdrasperiode.maxOrNull() ?: return false
        return YearMonth.parse(kontering.overføringsperiode).isAfter(maxOppdragsperiode)
    }

    private fun erPeriodeOverlappende(perioderForNyOppdrasperiode: List<YearMonth>, kontering: Kontering): Boolean {
        return perioderForNyOppdrasperiode.contains(YearMonth.parse(kontering.overføringsperiode))
    }

    private fun hentAllePerioderForOppdragsperiodeForSisteOverførtePeriode(
        oppdragsperiode: Oppdragsperiode,
        sisteOverførtePeriode: YearMonth
    ): List<YearMonth> {
        return PeriodeUtils.hentAllePerioderMellomDato(
            oppdragsperiode.periodeFra,
            oppdragsperiode.periodeTil,
            sisteOverførtePeriode
        ).filter { it.isBefore(sisteOverførtePeriode.plusMonths(1)) }
    }

    private fun erOverførtKonteringAlleredeKorrigert(
        kontering: Kontering,
        overførteKonteringerListe: List<Kontering>
    ): Boolean {
        if (overførteKonteringerListe.any {
            it.transaksjonskode == Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode && it.oppdragsperiode == kontering.oppdragsperiode && it.overføringsperiode == kontering.overføringsperiode
        }
        ) {
            return true
        }
        return false
    }

    fun hentAlleKonteringerForOppdrag(oppdrag: Oppdrag): List<Kontering> {
        val periodeListe = mutableListOf<Kontering>()
        oppdrag.oppdragsperioder.forEach { oppdragsperiode ->
            oppdragsperiode.konteringer.forEach { kontering ->
                periodeListe.add(kontering)
            }
        }
        return periodeListe
    }

    fun opprettManglendeKonteringerVedOppstartAvOpphørtOppdragsperiode(
        oppdrag: Oppdrag,
        oppdragsperiode: Oppdragsperiode,
        sisteOverførtePeriode: YearMonth
    ) {
        oppdrag.oppdragsperioder
            .filter { it.aktivTil == null && it.opphørendeOppdragsperiode }
            .forEach {
                val kontering = it.konteringer.maxByOrNull { kontering -> kontering.overføringsperiode } ?: return
                val nestePeriode = YearMonth.parse(kontering.overføringsperiode).plusMonths(1)
                val perioder = PeriodeUtils.hentAllePerioderMellomDato(
                    LocalDate.of(nestePeriode.year, nestePeriode.month, 1),
                    oppdragsperiode.periodeFra,
                    sisteOverførtePeriode
                )
                perioder.forEach { periode ->
                    it.konteringer = it.konteringer.plus(
                        Kontering(
                            oppdragsperiode = kontering.oppdragsperiode,
                            overføringsperiode = periode.toString(),
                            transaksjonskode = kontering.transaksjonskode,
                            type = Type.NY.toString(),
                            søknadType = kontering.søknadType
                        )
                    )
                }
            }
    }
}
