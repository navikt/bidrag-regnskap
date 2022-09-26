package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.dto.SkattKontering
import no.nav.bidrag.regnskap.dto.SkattKonteringerRequest
import no.nav.bidrag.regnskap.dto.SkattKonteringerResponse
import no.nav.bidrag.regnskap.dto.Transaksjonskode
import no.nav.bidrag.regnskap.dto.Type
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

private val LOGGER = LoggerFactory.getLogger(KonteringService::class.java)

@Service
class KonteringService(
  val skattConsumer: SkattConsumer, val persistenceService: PersistenceService
) {

  fun sendKontering(oppdragId: Int, periode: YearMonth): ResponseEntity<SkattKonteringerResponse> {
    LOGGER.info("Starter overføring av kontering til skatt..")

    val oppdrag = persistenceService.hentOppdrag(oppdragId)

    if (oppdrag.isEmpty) {
      return ResponseEntity(HttpStatus.NOT_FOUND)
    }

    val oppdragsPeriodeListe = hentOppdragsperioderForPeriode(oppdrag.get(), periode)

    when {
      oppdragsPeriodeListe.size > 1 -> {
        LOGGER.error("Fant flere enn 1 aktiv periode for oppdrag: $oppdragId! Noe har gått galt..")
        return ResponseEntity(HttpStatus.I_AM_A_TEAPOT) //TODO: Bedre feilhåndtering
      }

      oppdragsPeriodeListe.isEmpty() -> {
        LOGGER.info("Fant ingen aktive perioder for oppdrag: $oppdragId.")
        return ResponseEntity(HttpStatus.NO_CONTENT)
      }
    }

    val skattKonteringerRequest = opprettSkattKonteringerRequest(oppdrag.get(), oppdragsPeriodeListe.first(), periode)

    return skattConsumer.sendKontering(skattKonteringerRequest)
  }

  private fun opprettSkattKonteringerRequest(
    oppdrag: Oppdrag, oppdragsperiode: Oppdragsperiode, periode: YearMonth): SkattKonteringerRequest {

    val skattKonteringerRequest = SkattKonteringerRequest(
      listOf(
        SkattKontering(
          transaksjonskode = Transaksjonskode.A1, //TODO
          type = Type.NY, //TODO
          justering = null, //TODO
          gebyrRolle = null, //TODO
          gjelderIdent = oppdragsperiode.gjelderIdent,
          kravhaverIdent = oppdrag.kravhaverIdent,
          mottakerIdent = oppdragsperiode.mottakerIdent,
          skyldnerIdent = oppdrag.skyldnerIdent,
          belop = oppdragsperiode.belop,
          valuta = oppdragsperiode.valuta,
          periode = periode,
          vedtaksdato = oppdragsperiode.vedtaksdato,
          kjoredato = LocalDate.now(),
          saksbehandlerId = oppdragsperiode.opprettetAv,
          attestantId = oppdragsperiode.opprettetAv,
          tekst = oppdragsperiode.tekst,
          fagsystemId = "Bidrag-regnskap", //TODO
          delytelsesId = oppdragsperiode.delytelseId
        )
      )
    )
    return skattKonteringerRequest
  }

  private fun hentOppdragsperioderForPeriode(oppdrag: Oppdrag, periode: YearMonth): List<Oppdragsperiode> {
    val oppdragsperiodeListe = oppdrag.oppdragsperioder!!.filter { oppdragsperiode -> oppdragsperiode.aktiv }

    return oppdragsperiodeListe.filter {
      YearMonth.from(it.periodeFra).minusMonths(1).isBefore(periode) &&
          YearMonth.from(it.periodeTil).isAfter(periode) }
  }
}