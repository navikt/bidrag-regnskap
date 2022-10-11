package no.nav.bidrag.regnskap.service

import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.dto.OppdragRequest
import no.nav.bidrag.regnskap.dto.OppdragResponse
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.hendelse.SendKonteringerQueue
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors
import java.util.stream.Stream

private val LOGGER = LoggerFactory.getLogger(OppdragService::class.java)

@Service
class OppdragService(
  val persistenceService: PersistenceService,
  val oppdragsperiodeService: OppdragsperiodeService,
  val konteringService: KonteringService,
  val sendKonteringerQueue: SendKonteringerQueue
) {

  fun hentOppdrag(oppdragId: Int): OppdragResponse {
    val oppdrag = persistenceService.hentOppdrag(oppdragId).get()

    return OppdragResponse(
      oppdragId = oppdrag.oppdragId,
      stonadType = StonadType.valueOf(oppdrag.stonadType),
      kravhaverIdent = oppdrag.kravhaverIdent,
      skyldnerIdent = oppdrag.skyldnerIdent,
      referanse = oppdrag.referanse,
      sistOversendtePeriode = oppdrag.sistOversendtePeriode,
      endretTidspunkt = oppdrag.endretTidspunkt,
      oppdragsperioder = oppdragsperiodeService.hentOppdragsperioderMedKonteringer(oppdrag)
    )
  }

  @Transactional
  fun lagreOppdrag(oppdragRequest: OppdragRequest): Int {
    sjekkOmOppdragAlleredeErOpprettet(oppdragRequest)

    val oppdrag = opprettOppdrag(oppdragRequest)
    val oppdragsperiode = oppdragsperiodeService.opprettNyOppdragsperiode(oppdragRequest, oppdrag)
    val perioderForOppdrag = hentPeriodeForOppdrag(oppdragRequest)
    val konteringer = konteringService.opprettNyeKonteringer(perioderForOppdrag, oppdragsperiode)

    oppdrag.oppdragsperioder = listOf(oppdragsperiode)
    oppdragsperiode.konteringer = konteringer

    val oppdragId = persistenceService.lagreOppdrag(oppdrag)!!

    sendKonteringerQueue.leggTil(oppdragId)

    return oppdragId
  }

  @Transactional
  fun oppdaterOppdrag(oppdragRequest: OppdragRequest): Int {
    val oppdrag = persistenceService.hentOppdragPaUnikeIdentifikatorer(
      oppdragRequest.stonadType, oppdragRequest.kravhaverIdent, oppdragRequest.skyldnerIdent, oppdragRequest.referanse
    ).get()

    //TODO: Håndtere oppdatering av skyldnerIdent/kravhaver

    val overforteKonteringerListe = konteringService.finnAlleOverforteKontering(oppdrag)
    val erstattendeKonteringer = konteringService.opprettErstattendeKonteringer(
      overforteKonteringerListe,
      hentAllePerioderForOppdragRequest(oppdragRequest)
    )

    val erstattendeOppdragsperiode = oppdragsperiodeService.setAktivTilDatoPaOppdragsperiodeOgOpprettNyOppdragsperiode(
      oppdrag.oppdragsperioder, oppdragRequest
    )

    val nyeKonteringer =
      konteringService.opprettNyeKonteringer(hentPeriodeForOppdrag(oppdragRequest), erstattendeOppdragsperiode, true)

    erstattendeOppdragsperiode.konteringer = erstattendeKonteringer + nyeKonteringer
    oppdrag.oppdragsperioder = arrayListOf(erstattendeOppdragsperiode)
    oppdrag.endretTidspunkt = LocalDateTime.now()

    val oppdragId = persistenceService.lagreOppdrag(oppdrag)!!
    sendKonteringerQueue.leggTil(oppdragId)

    return oppdragId
  }

  private fun hentPeriodeForOppdrag(oppdragRequest: OppdragRequest): List<YearMonth> {
    val allePeriodeForOppdrag = hentAllePerioderForOppdragRequest(oppdragRequest)
    val sisteOverfortePeriodeForPalop = persistenceService.finnSisteOverfortePeriode()

    return allePeriodeForOppdrag.filter { it.isBefore(sisteOverfortePeriodeForPalop.plusMonths(1)) }
  }

  private fun sjekkOmOppdragAlleredeErOpprettet(oppdragRequest: OppdragRequest) {
    val oppdragOptional = persistenceService.hentOppdragPaUnikeIdentifikatorer(
      oppdragRequest.stonadType, oppdragRequest.kravhaverIdent, oppdragRequest.skyldnerIdent, oppdragRequest.referanse
    )

    if (oppdragOptional.isPresent) {
      val feilmelding =
        "Kombinasjonen av stonadType, kravhaverIdent, skyldnerIdent og referanse viser til et allerede opprettet oppdrag med id: ${oppdragOptional.get().oppdragId}"
      LOGGER.info(feilmelding + " For mer informasjon se secure logg.")
      SECURE_LOGGER.info(
        feilmelding + " " + "Oppdrag med stonadType: ${oppdragOptional.get().stonadType}, " + "kravhaverIdent: ${oppdragOptional.get().kravhaverIdent}, " + "skyldnerIdent: ${oppdragOptional.get().skyldnerIdent}, " + "referanse: ${oppdragOptional.get().referanse} " + "eksisterer allerede."
      )
      throw HttpClientErrorException(
        HttpStatus.BAD_REQUEST, feilmelding
      )
    }
  }

  private fun opprettOppdrag(oppdragRequest: OppdragRequest) = Oppdrag(
    stonadType = oppdragRequest.stonadType.toString(),
    kravhaverIdent = oppdragRequest.kravhaverIdent,
    skyldnerIdent = oppdragRequest.skyldnerIdent,
    referanse = oppdragRequest.referanse,
    utsattTilDato = oppdragRequest.utsattTilDato
  )

  private fun hentAllePerioderForOppdragRequest(oppdragRequest: OppdragRequest): List<YearMonth> {
    var periodeTil = oppdragRequest.periodeTil

    if (periodeTil == null) {
      val sisteOverfortePeriode = persistenceService.finnSisteOverfortePeriode()
      periodeTil = LocalDate.of(sisteOverfortePeriode.year, sisteOverfortePeriode.month, 1).plusMonths(1)
    }

    return Stream.iterate(oppdragRequest.periodeFra) { date: LocalDate -> date.plusMonths(1) }
      .limit(ChronoUnit.MONTHS.between(oppdragRequest.periodeFra, periodeTil))
      .map { it.format(DateTimeFormatter.ofPattern("yyyy-MM")) }.map { YearMonth.parse(it) }
      .collect(Collectors.toList())
  }
}