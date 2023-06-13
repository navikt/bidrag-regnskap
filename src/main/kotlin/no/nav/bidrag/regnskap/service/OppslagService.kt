package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.dto.enumer.Søknadstype
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.dto.enumer.Type
import no.nav.bidrag.regnskap.dto.oppdrag.KonteringResponse
import no.nav.bidrag.regnskap.dto.oppdrag.OppdragResponse
import no.nav.bidrag.regnskap.dto.oppdrag.OppdragsperiodeResponse
import no.nav.bidrag.regnskap.dto.oppdrag.OppslagAvOppdragPåSakIdResponse
import no.nav.bidrag.regnskap.dto.oppdrag.OverføringKonteringResponse
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.springframework.stereotype.Service

@Service
class OppslagService(
    private val persistenceService: PersistenceService
) {
    fun hentPåSakId(sakId: String): OppslagAvOppdragPåSakIdResponse? {
        val oppdrag = persistenceService.hentAlleOppdragPåSakId(sakId)

        if (oppdrag.isEmpty()) return null

        return OppslagAvOppdragPåSakIdResponse(oppdrag.map { hentOppdragResponse(it) })
    }

    fun hentOppdrag(oppdragId: Int): OppdragResponse? {
        val oppdrag = persistenceService.hentOppdrag(oppdragId) ?: return null

        return hentOppdragResponse(oppdrag)
    }

    fun hentOppdragResponse(oppdrag: Oppdrag): OppdragResponse {
        return OppdragResponse(
            oppdragId = oppdrag.oppdragId,
            type = oppdrag.stønadType,
            sakId = oppdrag.sakId,
            kravhaverIdent = oppdrag.kravhaverIdent,
            skyldnerIdent = oppdrag.skyldnerIdent,
            utsattTilTidspunkt = oppdrag.utsattTilDato.toString(),
            endretTidspunkt = oppdrag.endretTidspunkt.toString(),
            oppdragsperioder = hentOppdragsperioderMedKonteringer(oppdrag)
        )
    }

    fun hentOppdragsperioderMedKonteringer(oppdrag: Oppdrag): List<OppdragsperiodeResponse> {
        return (oppdrag.oppdragsperioder).map {
            OppdragsperiodeResponse(
                oppdragsperiodeId = it.oppdragsperiodeId,
                oppdragId = it.oppdrag?.oppdragId,
                vedtakId = it.vedtakId,
                referanse = it.referanse,
                gjelderIdent = it.gjelderIdent,
                mottakerIdent = it.mottakerIdent,
                belop = it.beløp,
                valuta = it.valuta,
                periodeFra = it.periodeFra.toString(),
                periodeTil = it.periodeTil.toString(),
                vedtaksdato = it.vedtaksdato.toString(),
                opprettetAv = it.opprettetAv,
                delytelseId = it.delytelseId,
                eksternReferanse = it.eksternReferanse,
                aktivTil = it.aktivTil.toString(),
                konteringer = hentKonteringer(it),
                opphørendeOppdragsperiode = it.opphørendeOppdragsperiode
            )
        }
    }

    fun hentKonteringer(oppdragsperiode: Oppdragsperiode): List<KonteringResponse> {
        return oppdragsperiode.konteringer.map {
            KonteringResponse(
                konteringId = it.konteringId,
                oppdragsperiodeId = it.oppdragsperiode?.oppdragsperiodeId,
                transaksjonskode = Transaksjonskode.valueOf(it.transaksjonskode),
                overforingsperiode = it.overføringsperiode,
                overforingstidspunkt = it.overføringstidspunkt.toString(),
                behandlingsstatusOkTidspunkt = it.behandlingsstatusOkTidspunkt.toString(),
                type = Type.valueOf(it.type),
                soknadType = Søknadstype.valueOf(it.søknadType),
                sendtIPalopsfil = it.sendtIPåløpsfil,
                overføringKonteringer = hentOverføringKontering(it),
                sisteReferansekode = it.sisteReferansekode,
                opprettetTidspunkt = it.opprettetTidspunkt.toString(),
                vedtakId = it.vedtakId
            )
        }
    }

    private fun hentOverføringKontering(kontering: Kontering): List<OverføringKonteringResponse> {
        return kontering.overføringKontering?.map {
            OverføringKonteringResponse(
                overføringId = it.overføringId,
                konteringId = kontering.konteringId,
                referansekode = it.referansekode,
                feilmelding = it.feilmelding,
                tidspunkt = it.tidspunkt,
                kanal = it.kanal
            )
        } ?: emptyList()
    }
}