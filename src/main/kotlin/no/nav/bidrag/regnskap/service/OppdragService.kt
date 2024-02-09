package no.nav.bidrag.regnskap.service

import no.nav.bidrag.commons.util.SjekkForNyIdent
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.regnskap.consumer.SakConsumer
import no.nav.bidrag.regnskap.dto.patch.OppdaterUtsattTilDatoRequest
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.util.IdentUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val LOGGER = LoggerFactory.getLogger(OppdragService::class.java)

@Service
class OppdragService(
    private val persistenceService: PersistenceService,
    private val oppdragsperiodeService: OppdragsperiodeService,
    private val konteringService: KonteringService,
    private val sakConsumer: SakConsumer,
) {

    @Transactional
    fun lagreHendelse(hendelse: Hendelse, erEngangsbeløp: Boolean = false): Int? {
        val oppdrag = hentOppdrag(hendelse)

        return lagreEllerOppdaterOppdrag(oppdrag, hendelse, erEngangsbeløp)
    }

    fun lagreEllerOppdaterOppdrag(hentetOppdrag: Oppdrag?, hendelse: Hendelse, erEngangsbeløp: Boolean): Int? {
        // Dette er en edge case hvor vedtak som inneholder gebyrfritak, eller andre stønadstyper som kun blir sendt inn med avslag, kommer med beløp null og ikke eksisterer fra før av. Disse skal ikke opprettes.
        if (hentetOppdrag == null && hendelse.periodeListe.all { it.beløp == null }) {
            LOGGER.info("Hendelse for vedtak: ${hendelse.vedtakId} har fått fritak for ${hendelse.type}")
            return null
        }

        val erOppdatering = hentetOppdrag != null
        val oppdrag = hentetOppdrag ?: opprettOppdrag(hendelse)
        val sisteOverførtePeriode = persistenceService.finnSisteOverførtePeriode()

        // For oppdateringer på engangsbeløp skal fra og til dato være lik det opprinnelige engangsbeløpet.
        if (erOppdatering && erEngangsbeløp) {
            settNyPeriodeFraOgTilDatoForOppdateringPåEngangsbeløp(hendelse, hentetOppdrag)
        }

        hendelse.periodeListe.forEach { periode ->
            val oppdragsperiode = oppdragsperiodeService.opprettNyOppdragsperiode(hendelse, periode, oppdrag)
            if (erOppdatering) {
                konteringService.opprettKorreksjonskonteringer(oppdrag, oppdragsperiode, sisteOverførtePeriode, hendelse)
                konteringService.opprettManglendeKonteringerVedOppstartAvOpphørtOppdragsperiode(
                    oppdrag,
                    oppdragsperiode,
                    sisteOverførtePeriode,
                    hendelse,
                )
            }
            oppdragsperiodeService.settAktivTilDatoPåEksisterendeOppdragsperioder(oppdrag, oppdragsperiode.periodeFra)
            oppdrag.oppdragsperioder = oppdrag.oppdragsperioder.plus(oppdragsperiode)
            konteringService.opprettNyeKonteringerPåOppdragsperiode(
                oppdragsperiode,
                hendelse,
                sisteOverførtePeriode,
            )
        }

        oppdatererVerdierPåOppdrag(hendelse, oppdrag)
        val oppdragId = persistenceService.lagreOppdrag(oppdrag)

        LOGGER.debug("Oppdrag med ID: $oppdragId er ${if (erOppdatering) "oppdatert." else "opprettet."}")

        return oppdragId
    }

    private fun settNyPeriodeFraOgTilDatoForOppdateringPåEngangsbeløp(hendelse: Hendelse, hentetOppdrag: Oppdrag?) {
        hendelse.periodeListe.forEach {
            it.periodeFomDato = hentetOppdrag!!.oppdragsperioder.first().periodeFra
            it.periodeTilDato = hentetOppdrag.oppdragsperioder.first().periodeTil
        }
    }

    private fun hentOppdrag(hendelse: Hendelse): Oppdrag? {
        if (hendelse.referanse != null && hendelse.omgjørVedtakId != null) {
            return persistenceService.hentOppdragPåReferanseOgOmgjørVedtakId(hendelse.referanse, hendelse.omgjørVedtakId)
        } else if (hendelse.referanse == null) {
            return persistenceService.hentOppdragPaUnikeIdentifikatorer(
                hendelse.type,
                hendelse.kravhaverIdent,
                hendelse.skyldnerIdent,
                hendelse.sakId,
            )
        }
        return null
    }

    private fun opprettOppdrag(hendelse: Hendelse): Oppdrag {
        LOGGER.debug("Fant ikke eksisterende oppdrag for vedtakID: ${hendelse.vedtakId}. Opprettet nytt oppdrag..")
        return Oppdrag(
            stønadType = hendelse.type,
            sakId = hendelse.sakId,
            kravhaverIdent = hendelse.kravhaverIdent,
            skyldnerIdent = hendelse.skyldnerIdent,
            gjelderIdent = sakConsumer.hentBmFraSak(hendelse.sakId),
            mottakerIdent = hendelse.mottakerIdent,
            utsattTilDato = hendelse.utsattTilDato,
        )
    }

    fun oppdatererVerdierPåOppdrag(hendelse: Hendelse, oppdrag: Oppdrag) {
        oppdrag.endretTidspunkt = LocalDateTime.now()
        // Utsatt til dato skal ikke kunne forkortes eller fjernes om først satt via vedtak
        if (oppdrag.utsattTilDato == null || hendelse.utsattTilDato?.isAfter(oppdrag.utsattTilDato) == true) {
            oppdrag.utsattTilDato = hendelse.utsattTilDato
        }
        oppdrag.mottakerIdent = hendelse.mottakerIdent
    }

    @Transactional
    fun patchMottaker(saksnummer: Saksnummer, @SjekkForNyIdent kravhaver: Personident, mottaker: Personident) {
        val oppdragPåSaksnummerOgKravhaver = persistenceService.hentOppdragPåSaksnummerOgKravhaver(saksnummer, kravhaver)

        val stønadstyperSomSkalOppdatereMedBmEllerRm = arrayOf(
            Stønadstype.FORSKUDD.name,
            Stønadstype.BIDRAG18AAR.name,
            Stønadstype.BIDRAG.name,
            Stønadstype.OPPFOSTRINGSBIDRAG.name,
            Stønadstype.MOTREGNING.name,
            Engangsbeløptype.SÆRTILSKUDD.name,
            "SAERTILSKUDD",
            Engangsbeløptype.ETTERGIVELSE.name,
            Engangsbeløptype.DIREKTE_OPPGJØR.name,
            "DIREKTE_OPPGJOR",
        )

        val stønaderSomSkalOppdatereTilNavIdent = arrayOf(
            Engangsbeløptype.GEBYR_MOTTAKER.name,
            Engangsbeløptype.GEBYR_SKYLDNER.name,
            Engangsbeløptype.TILBAKEKREVING.name,
            Engangsbeløptype.ETTERGIVELSE_TILBAKEKREVING.name,
        )

        oppdragPåSaksnummerOgKravhaver.forEach { oppdrag ->
            when (oppdrag.stønadType) {
                in stønadstyperSomSkalOppdatereMedBmEllerRm -> {
                    oppdrag.mottakerIdent = mottaker.verdi
                }
                in stønaderSomSkalOppdatereTilNavIdent -> {
                    oppdrag.mottakerIdent = IdentUtils.NAV_TSS_IDENT
                }
                Stønadstype.EKTEFELLEBIDRAG.name -> {
                    oppdrag.mottakerIdent = oppdrag.kravhaverIdent ?: ""
                }
            }
        }
    }

    @Transactional
    fun oppdaterUtsattTilDato(oppdaterUtsattTilDatoRequest: OppdaterUtsattTilDatoRequest): Int? {
        val oppdrag = persistenceService.hentOppdrag(oppdaterUtsattTilDatoRequest.oppdragsid) ?: return null

        oppdrag.utsattTilDato = oppdaterUtsattTilDatoRequest.nyUtsattTilDato

        return oppdrag.oppdragId
    }
}
