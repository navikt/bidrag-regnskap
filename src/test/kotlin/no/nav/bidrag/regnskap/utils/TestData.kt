package no.nav.bidrag.regnskap.utils

import no.nav.bidrag.behandling.felles.dto.vedtak.Engangsbelop
import no.nav.bidrag.behandling.felles.dto.vedtak.Stonadsendring
import no.nav.bidrag.behandling.felles.dto.vedtak.VedtakHendelse
import no.nav.bidrag.behandling.felles.enums.EngangsbelopType
import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.behandling.felles.enums.VedtakType
import no.nav.bidrag.regnskap.dto.enumer.SøknadType
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.dto.enumer.Type
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.dto.vedtak.Periode
import no.nav.bidrag.regnskap.persistence.entity.Driftsavvik
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.entity.OverføringKontering
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.random.Random

object TestData {

  fun opprettOppdrag(
    oppdragId: Int? = null,
    stonadType: StonadType = StonadType.BIDRAG,
    vedtakType: VedtakType = VedtakType.MANUELT,
    skyldnerIdent: String = TestDataGenerator.genererPersonnummer(),
    oppdragsperioder: List<Oppdragsperiode>? = listOf(opprettOppdragsperiode()),
    kravhaverIdent: String = TestDataGenerator.genererPersonnummer(),
    utsattTilDato: LocalDate? = null,
    sistOversendtePeriode: String? = null,
    endretTidspunkt: LocalDateTime? = null,
    engangsbeløpId: Int? = null
  ): Oppdrag {
    return Oppdrag(
      oppdragId = oppdragId,
      stønadType = stonadType.toString(),
      vedtakType = vedtakType.toString(),
      skyldnerIdent = skyldnerIdent,
      oppdragsperioder = oppdragsperioder,
      kravhaverIdent = kravhaverIdent,
      utsattTilDato = utsattTilDato,
      sistOversendtePeriode = sistOversendtePeriode,
      endretTidspunkt = endretTidspunkt,
      engangsbeløpId = engangsbeløpId
    )
  }

  fun opprettVedtakhendelse(
    vedtakType: VedtakType = VedtakType.MANUELT,
    vedtakId: Int = 12345,
    vedtakDato: LocalDate = LocalDate.now(),
    enhetId: String = "Oslo 2",
    eksternReferanse: String = "Utenlandsavdeling notat",
    utsattTilDato: LocalDate = LocalDate.now().plusDays(3),
    opprettetAv: String = "Saksbehandler",
    opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    stonadsendringListe: List<Stonadsendring> = listOf(opprettStonadsending()),
    engangsbelopListe: List<Engangsbelop> = listOf(opprettEngangsbelop())
  ): VedtakHendelse {
    return VedtakHendelse(
      vedtakType = vedtakType,
      vedtakId = vedtakId,
      vedtakDato = vedtakDato,
      enhetId = enhetId,
      eksternReferanse = eksternReferanse,
      utsattTilDato = utsattTilDato,
      opprettetAv = opprettetAv,
      opprettetTidspunkt = opprettetTidspunkt,
      stonadsendringListe = stonadsendringListe,
      engangsbelopListe = engangsbelopListe
    )
  }

  fun opprettEngangsbelop(
    engangsbeløpId: Int = 123,
    endrerEngangsbeløpId: Int? = null,
    type: EngangsbelopType = EngangsbelopType.GEBYR_SKYLDNER,
    sakId: String = "Sak123",
    skyldnerId: String = TestDataGenerator.genererPersonnummer(),
    kravhaverId: String = TestDataGenerator.genererPersonnummer(),
    mottakerId: String = TestDataGenerator.genererPersonnummer(),
    belop: BigDecimal = BigDecimal.valueOf(1200),
    valutakode: String = "NOK",
    resultatkode: String = "KOS",
    referanse: String = UUID.randomUUID().toString()
  ): Engangsbelop {
    return Engangsbelop(
      engangsbelopId = engangsbeløpId,
      endrerEngangsbelopId = endrerEngangsbeløpId,
      type = type,
      sakId = sakId,
      skyldnerId = skyldnerId,
      kravhaverId = kravhaverId,
      mottakerId = mottakerId,
      belop = belop,
      valutakode = valutakode,
      resultatkode = resultatkode,
      referanse = referanse
    )
  }

  fun opprettStonadsending(
    stonadType: StonadType = StonadType.BIDRAG,
    sakId: String = "SAK1",
    skyldnerId: String = TestDataGenerator.genererPersonnummer(),
    kravhaverId: String = TestDataGenerator.genererPersonnummer(),
    mottakerId: String = TestDataGenerator.genererPersonnummer(),
    periodeListe: List<no.nav.bidrag.behandling.felles.dto.vedtak.Periode> = listOf(opprettPeriode()),
    indeksreguleringAar: String? = null,
    opphortFra: LocalDate? = null
  ): Stonadsendring {
    return Stonadsendring(
      stonadType = stonadType,
      sakId = sakId,
      skyldnerId = skyldnerId,
      kravhaverId = kravhaverId,
      mottakerId = mottakerId,
      periodeListe = periodeListe,
      indeksreguleringAar = indeksreguleringAar,
      opphortFra = opphortFra
    )
  }

  fun opprettPeriode(
    periodeFomDato: LocalDate = LocalDate.now().minusMonths(2).withDayOfMonth(1),
    periodeTilDato: LocalDate? = LocalDate.now(),
    belop: BigDecimal = BigDecimal.valueOf(7500),
    valutakode: String = "NOK",
    resultatkode: String = "ABC",
    referanse: String? = UUID.randomUUID().toString()
  ): no.nav.bidrag.behandling.felles.dto.vedtak.Periode {
    return no.nav.bidrag.behandling.felles.dto.vedtak.Periode(
      periodeFomDato = periodeFomDato,
      periodeTilDato = periodeTilDato,
      belop = belop,
      valutakode = valutakode,
      resultatkode = resultatkode,
      referanse = referanse
    )
  }

  fun opprettHendelse(
    engangsbelopId: Int? = null,
    type: String = StonadType.BIDRAG.name,
    vedtakType: VedtakType = VedtakType.MANUELT,
    kravhaverIdent: String = TestDataGenerator.genererPersonnummer(),
    skyldnerIdent: String = TestDataGenerator.genererPersonnummer(),
    mottakerIdent: String = TestDataGenerator.genererPersonnummer(),
    sakId: String = "Sak123",
    vedtakId: Int = 12345,
    vedtakDato: LocalDate = LocalDate.now(),
    opprettetAv: String = "SaksbehandlerId",
    eksternReferanse: String? = "UTENLANDSREFERANSE",
    utsattTilDato: LocalDate? = LocalDate.now().plusDays(7),
    periodeListe: List<Periode> = listOf(opprettPeriodeDomene())
  ): Hendelse {
    return Hendelse(
      engangsbelopId = engangsbelopId,
      type = type,
      vedtakType = vedtakType,
      kravhaverIdent = kravhaverIdent,
      skyldnerIdent = skyldnerIdent,
      mottakerIdent = mottakerIdent,
      sakId = sakId,
      vedtakId = vedtakId,
      vedtakDato = vedtakDato,
      opprettetAv = opprettetAv,
      eksternReferanse = eksternReferanse,
      utsattTilDato = utsattTilDato,
      periodeListe = periodeListe
    )
  }

  fun opprettPeriodeDomene(
    beløp: BigDecimal? = BigDecimal.valueOf(7500.0),
    valutakode: String? = "NOK",
    periodeFomDato: LocalDate = LocalDate.now().minusMonths(2).withDayOfMonth(1),
    periodeTilDato: LocalDate? = LocalDate.now(),
    referanse: Int? = Random.nextInt()
  ): Periode {
    return Periode(
      beløp = beløp,
      valutakode = valutakode,
      periodeFomDato = periodeFomDato,
      periodeTilDato = periodeTilDato,
      referanse = referanse
    )
  }

  fun opprettOppdragsperiode(
    oppdragsperiodeId: Int? = null,
    oppdrag: Oppdrag? = null,
    sakId: String = "123456",
    vedtakId: Int = 654321,
    gjelderIdent: String = TestDataGenerator.genererPersonnummer(),
    mottakerIdent: String = TestDataGenerator.genererPersonnummer(),
    belop: BigDecimal = BigDecimal(7500),
    valuta: String = "NOK",
    periodeFra: LocalDate = LocalDate.now().minusMonths(1),
    periodeTil: LocalDate = LocalDate.now().plusMonths(1),
    vedtaksdato: LocalDate = LocalDate.now(),
    opprettetAv: String = "Saksbehandler",
    delytelseId: Int? = Random.nextInt(),
    aktivTil: LocalDate? = null,
    konteringer: List<Kontering>? = listOf(opprettKontering())

  ): Oppdragsperiode {
    return Oppdragsperiode(
      oppdragsperiodeId = oppdragsperiodeId,
      oppdrag = oppdrag,
      sakId = sakId,
      vedtakId = vedtakId,
      gjelderIdent = gjelderIdent,
      mottakerIdent = mottakerIdent,
      beløp = belop,
      valuta = valuta,
      periodeFra = periodeFra,
      periodeTil = periodeTil,
      vedtaksdato = vedtaksdato,
      opprettetAv = opprettetAv,
      delytelseId = delytelseId,
      aktivTil = aktivTil,
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
    søknadType: String = SøknadType.EN.name,
    sendtIPalopsfil: Boolean = false,
    overføringKontering: List<OverføringKontering>? = listOf(opprettOverføringKontering())
  ): Kontering {
    return Kontering(
      konteringId = konteringId,
      oppdragsperiode = oppdragsperiode,
      transaksjonskode = transaksjonskode,
      overføringsperiode = overforingsperiode,
      overføringstidspunkt = overforingstidspunkt,
      type = type,
      søknadType = søknadType,
      sendtIPåløpsfil = sendtIPalopsfil,
      overføringKontering = overføringKontering
    )
  }

  fun opprettOverføringKontering(
    overføringId: Int? = null,
    kontering: Kontering? = null,
    referansekode: String? = null,
    feilmelding: String? = null,
    tidspunkt: LocalDateTime = LocalDateTime.now(),
    kanal: String = "REST"
  ): OverføringKontering {
    return OverføringKontering(
      overføringId = overføringId,
      kontering = kontering,
      referansekode = referansekode,
      feilmelding = feilmelding,
      tidspunkt = tidspunkt,
      kanal = kanal
    )
  }

  fun opprettPåløp(
    påløpId: Int? = null,
    kjøredato: LocalDateTime = LocalDateTime.now(),
    fullførtTidspunkt: LocalDateTime? = null,
    forPeriode: String = "2022-01"
  ): Påløp {
    return Påløp(
      påløpId = påløpId,
      kjøredato = kjøredato,
      fullførtTidspunkt = fullførtTidspunkt,
      forPeriode = forPeriode
    )
  }

  fun opprettDriftsavvik(
    driftsavvikId: Int? = null,
    påløpId: Int? = null,
    tidspunktFra: LocalDateTime = LocalDateTime.now(),
    tidspunktTil: LocalDateTime? = LocalDateTime.now().plusHours(1),
    opprettetAv: String? = "Manuelt REST",
    årsak: String? = "Feil ved overføringer"
  ): Driftsavvik {
    return Driftsavvik(
      driftsavvikId = driftsavvikId,
      påløpId = påløpId,
      tidspunktFra = tidspunktFra,
      tidspunktTil = tidspunktTil,
      opprettetAv = opprettetAv,
      årsak = årsak
    )
  }
}