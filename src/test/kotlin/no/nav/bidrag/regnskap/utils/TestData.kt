package no.nav.bidrag.regnskap.utils

import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.domene.enums.regnskap.Søknadstype
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.domene.enums.regnskap.Type
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.dto.vedtak.Periode
import no.nav.bidrag.regnskap.persistence.entity.Driftsavvik
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.random.Random

object TestData {

    fun opprettOppdrag(
        oppdragId: Int = 0,
        stonadType: Stønadstype? = Stønadstype.BIDRAG,
        engangsbelopType: Engangsbeløptype? = null,
        sakId: String = "123456",
        skyldnerIdent: String = PersonidentGenerator.genererFødselsnummer(),
        oppdragsperioder: List<Oppdragsperiode> = listOf(opprettOppdragsperiode()),
        kravhaverIdent: String = PersonidentGenerator.genererFødselsnummer(),
        mottakerIdent: String = PersonidentGenerator.genererFødselsnummer(),
        gjelderIdent: String = PersonidentGenerator.genererFødselsnummer(),
        utsattTilDato: LocalDate? = null,
        endretTidspunkt: LocalDateTime? = null,
    ): Oppdrag {
        return Oppdrag(
            oppdragId = oppdragId,
            stønadType = stonadType?.toString() ?: engangsbelopType.toString(),
            sakId = sakId,
            skyldnerIdent = skyldnerIdent,
            oppdragsperioder = oppdragsperioder,
            kravhaverIdent = kravhaverIdent,
            gjelderIdent = gjelderIdent,
            mottakerIdent = mottakerIdent,
            utsattTilDato = utsattTilDato,
            endretTidspunkt = endretTidspunkt,
        )
    }

    fun opprettHendelse(
        type: String = Stønadstype.BIDRAG.name,
        vedtakType: Vedtakstype = Vedtakstype.FASTSETTELSE,
        kravhaverIdent: String = PersonidentGenerator.genererFødselsnummer(),
        skyldnerIdent: String = PersonidentGenerator.genererFødselsnummer(),
        mottakerIdent: String = PersonidentGenerator.genererFødselsnummer(),
        sakId: String = "Sak123",
        vedtakId: Int = 12345,
        vedtakDato: LocalDate = LocalDate.now(),
        opprettetAv: String = "SaksbehandlerId",
        enhetsnummer: Enhetsnummer? = Enhetsnummer("1234"),
        eksternReferanse: String? = "UTENLANDSREFERANSE",
        utsattTilDato: LocalDate? = LocalDate.now().plusDays(7),
        periodeListe: List<Periode> = listOf(opprettPeriodeDomene()),
    ): Hendelse {
        return Hendelse(
            type = type,
            vedtakType = vedtakType,
            kravhaverIdent = kravhaverIdent,
            skyldnerIdent = skyldnerIdent,
            mottakerIdent = mottakerIdent,
            sakId = sakId,
            vedtakId = vedtakId,
            vedtakDato = vedtakDato,
            opprettetAv = opprettetAv,
            enhetsnummer = enhetsnummer,
            eksternReferanse = eksternReferanse,
            utsattTilDato = utsattTilDato,
            periodeListe = periodeListe,
        )
    }

    fun opprettPeriodeDomene(
        beløp: BigDecimal? = BigDecimal.valueOf(7500.0),
        valutakode: String? = "NOK",
        periodeFomDato: LocalDate = LocalDate.now().minusMonths(2).withDayOfMonth(1),
        periodeTilDato: LocalDate? = LocalDate.now(),
        referanse: Int? = Random.nextInt(),
    ): Periode {
        return Periode(
            beløp = beløp,
            valutakode = valutakode,
            periodeFomDato = periodeFomDato,
            periodeTilDato = periodeTilDato,
            delytelsesId = referanse,
        )
    }

    fun opprettOppdragsperiode(
        oppdragsperiodeId: Int = 0,
        oppdrag: Oppdrag? = null,
        vedtakId: Int = 654321,
        vedtakType: Vedtakstype = Vedtakstype.FASTSETTELSE,
        referanse: String? = null,
        belop: BigDecimal = BigDecimal(7500),
        valuta: String = "NOK",
        periodeFra: LocalDate = LocalDate.now().minusMonths(1),
        periodeTil: LocalDate? = LocalDate.now().plusMonths(1),
        vedtaksdato: LocalDate = LocalDate.now(),
        opprettetAv: String = "Saksbehandler",
        konteringerFullførtOpprettet: Boolean = false,
        delytelseId: Int? = Random.nextInt(),
        aktivTil: LocalDate? = null,
        konteringer: List<Kontering> = listOf(opprettKontering()),
    ): Oppdragsperiode {
        return Oppdragsperiode(
            oppdragsperiodeId = oppdragsperiodeId,
            oppdrag = oppdrag,
            vedtakId = vedtakId,
            referanse = referanse,
            vedtakType = vedtakType.toString(),
            beløp = belop,
            valuta = valuta,
            periodeFra = periodeFra,
            periodeTil = periodeTil,
            vedtaksdato = vedtaksdato,
            opprettetAv = opprettetAv,
            konteringerFullførtOpprettet = konteringerFullførtOpprettet,
            delytelseId = delytelseId,
            aktivTil = aktivTil,
            konteringer = konteringer,
        )
    }

    fun opprettKontering(
        konteringId: Int = 0,
        oppdragsperiode: Oppdragsperiode? = null,
        transaksjonskode: String = Transaksjonskode.A1.name,
        overforingsperiode: String = YearMonth.now().toString(),
        overforingstidspunkt: LocalDateTime? = null,
        type: String = Type.NY.name,
        søknadstype: String = Søknadstype.EN.name,
        sendtIPalopsperiode: String? = null,
        opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
        vedtakId: Int = 654321,
    ): Kontering {
        return Kontering(
            konteringId = konteringId,
            oppdragsperiode = oppdragsperiode,
            transaksjonskode = transaksjonskode,
            overføringsperiode = overforingsperiode,
            overføringstidspunkt = overforingstidspunkt,
            type = type,
            søknadType = søknadstype,
            sendtIPåløpsperiode = sendtIPalopsperiode,
            opprettetTidspunkt = opprettetTidspunkt,
            vedtakId = vedtakId,
        )
    }

    fun opprettPåløp(
        påløpId: Int = 0,
        kjøredato: LocalDateTime = LocalDateTime.now(),
        fullførtTidspunkt: LocalDateTime? = null,
        forPeriode: String = "2022-01",
    ): Påløp {
        return Påløp(
            påløpId = påløpId,
            kjøredato = kjøredato,
            fullførtTidspunkt = fullførtTidspunkt,
            forPeriode = forPeriode,
        )
    }

    fun opprettDriftsavvik(
        driftsavvikId: Int = 0,
        påløpId: Int? = null,
        tidspunktFra: LocalDateTime = LocalDateTime.now(),
        tidspunktTil: LocalDateTime? = LocalDateTime.now().plusHours(1),
        opprettetAv: String? = "Manuelt REST",
        årsak: String? = "Feil ved overføringer",
    ): Driftsavvik {
        return Driftsavvik(
            driftsavvikId = driftsavvikId,
            påløpId = påløpId,
            tidspunktFra = tidspunktFra,
            tidspunktTil = tidspunktTil,
            opprettetAv = opprettetAv,
            årsak = årsak,
        )
    }
}
