package no.nav.bidrag.regnskap.utils

import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.regnskap.dto.OppdragRequest
import no.nav.bidrag.regnskap.dto.Transaksjonskode
import no.nav.bidrag.regnskap.dto.Type
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.entity.OverforingKontering
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

object TestData {

  fun opprettOppdragRequest(
    stonadType: StonadType = StonadType.BIDRAG,
    kravhaverIdent: String = TestDataGenerator.genererPersonnummer(),
    skyldnerIdent: String = TestDataGenerator.genererPersonnummer(),
    sakId: Int = 123456,
    vedtakId: Int = 654321,
    gjelderIdent: String = TestDataGenerator.genererPersonnummer(),
    mottakerIdent: String = TestDataGenerator.genererPersonnummer(),
    belop: Int = 7500,
    valuta: String = "NOK",
    periodeFra: LocalDate = LocalDate.now().minusMonths(6).withDayOfMonth(1),
    periodeTil: LocalDate = LocalDate.now().plusMonths(6).withDayOfMonth(1),
    vedtakDato: LocalDate = LocalDate.now(),
    opprettetAv: String = "SaksbehandlerId",
    delytelseId: String? = null,
    referanse: String? = null,
    utsattTilDato: LocalDate? = null,
    tekst: String? = null

  ): OppdragRequest {
    return OppdragRequest(
      stonadType = stonadType,
      kravhaverIdent = kravhaverIdent,
      skyldnerIdent = skyldnerIdent,
      sakId = sakId,
      vedtakId = vedtakId,
      gjelderIdent = gjelderIdent,
      mottakerIdent = mottakerIdent,
      belop = belop,
      valuta = valuta,
      periodeFra = periodeFra,
      periodeTil = periodeTil,
      vedtaksdato = vedtakDato,
      opprettetAv = opprettetAv,
      delytelseId = delytelseId,
      referanse = referanse,
      utsattTilDato = utsattTilDato,
      tekst = tekst
    )
  }

  fun opprettOppdrag(
    oppdragId: Int? = null,
    stonadType: StonadType = StonadType.BIDRAG,
    skyldnerIdent: String = TestDataGenerator.genererPersonnummer(),
    oppdragsperioder: List<Oppdragsperiode>? = listOf(opprettOppdragsperiode()),
    kravhaverIdent: String = TestDataGenerator.genererPersonnummer(),
    referanse: String? = null,
    utsattTilDato: LocalDate? = null,
    sistOversendtePeriode: String? = null,
    endretTidspunkt: LocalDateTime? = null
  ): Oppdrag {
    return Oppdrag(
      oppdragId = oppdragId,
      stonadType = stonadType.toString(),
      skyldnerIdent = skyldnerIdent,
      oppdragsperioder = oppdragsperioder,
      kravhaverIdent = kravhaverIdent,
      referanse = referanse,
      utsattTilDato = utsattTilDato,
      sistOversendtePeriode = sistOversendtePeriode,
      endretTidspunkt = endretTidspunkt
    )
  }

  fun opprettOppdragsperiode(
    oppdragsperiodeId: Int? = null,
    oppdrag: Oppdrag? = null,
    sakId: Int = 123456,
    vedtakId: Int = 654321,
    gjelderIdent: String = TestDataGenerator.genererPersonnummer(),
    mottakerIdent: String = TestDataGenerator.genererPersonnummer(),
    belop: Int = 7500,
    valuta: String = "NOK",
    periodeFra: LocalDate = LocalDate.now().minusMonths(1),
    periodeTil: LocalDate = LocalDate.now().plusMonths(1),
    vedtaksdato: LocalDate = LocalDate.now(),
    opprettetAv: String = "Saksbehandler",
    delytelseId: String = "DelytelseId",
    aktivTil: LocalDate? = null,
    erstatterPeriode: Int? = null,
    tekst: String? = null,
    konteringer: List<Kontering> = listOf(opprettKontering())

  ): Oppdragsperiode {
    return Oppdragsperiode(
      oppdragsperiodeId = oppdragsperiodeId,
      oppdrag = oppdrag,
      sakId = sakId,
      vedtakId = vedtakId,
      gjelderIdent = gjelderIdent,
      mottakerIdent = mottakerIdent,
      belop = belop,
      valuta = valuta,
      periodeFra = periodeFra,
      periodeTil = periodeTil,
      vedtaksdato = vedtaksdato,
      opprettetAv = opprettetAv,
      delytelseId = delytelseId,
      aktivTil = aktivTil,
      erstatterPeriode = erstatterPeriode,
      tekst = tekst,
      konteringer = konteringer
    )
  }

  fun opprettKontering(
    konteringId: Int? = null,
    oppdragsperiode: Oppdragsperiode? = null,
    transaksjonskode: String = Transaksjonskode.A1.toString(),
    overforingsperiode: String = YearMonth.now().toString(),
    overforingstidspunkt: LocalDateTime? = null,
    type: String = Type.NY.toString(),
    justering: String? = null,
    gebyrRolle: String? = null,
    sendtIPalopsfil: Boolean = false,
    overforingKontering: List<OverforingKontering> = listOf(opprettOverforingKontering())

    ): Kontering {
    return Kontering(
      konteringId = konteringId,
      oppdragsperiode = oppdragsperiode,
      transaksjonskode = transaksjonskode,
      overforingsperiode = overforingsperiode,
      overforingstidspunkt = overforingstidspunkt,
      type = type,
      justering = justering,
      gebyrRolle = gebyrRolle,
      sendtIPalopsfil = sendtIPalopsfil,
      overforingKontering = overforingKontering
    )
  }

  private fun opprettOverforingKontering(
    overforingId: Int? = null,
    kontering: Kontering? = null,
    referansekode: String? = null,
    feilmelding: String? = null,
    tidspunkt: LocalDateTime = LocalDateTime.now(),
    kanal: String = "REST"
  ): OverforingKontering {
    return OverforingKontering(
      overforingId = overforingId,
      kontering = kontering,
      referansekode = referansekode,
      feilmelding = feilmelding,
      tidspunkt = tidspunkt,
      kanal = kanal
    )
  }
}