package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.dto.SkattKontering
import no.nav.bidrag.regnskap.dto.SkattKonteringerRequest
import no.nav.bidrag.regnskap.dto.SkattKonteringerResponse
import no.nav.bidrag.regnskap.dto.Transaksjonskode
import no.nav.bidrag.regnskap.dto.Type
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

    val oppdragsPeriodeListe = hentOppdragsperioderForPeriode(oppdragId, periode)

    when {
      oppdragsPeriodeListe.size > 1 -> {
        LOGGER.error("Fant flere enn 1 aktiv periode for oppdrag: $oppdragId! Noe har gått veldig galt..")
        return ResponseEntity(HttpStatus.I_AM_A_TEAPOT) //TODO: Bedre feilhåndtering
      }

      oppdragsPeriodeListe.isEmpty() -> {
        LOGGER.info("Fant ingen aktive perioder for oppdrag: $oppdragId.")
        return ResponseEntity(HttpStatus.NO_CONTENT)
      }
    }

    val skattKonteringerRequest = opprettSkattKonteringerRequest(oppdragId, oppdragsPeriodeListe, periode)

    return skattConsumer.sendKontering(skattKonteringerRequest)
  }

  private fun opprettSkattKonteringerRequest(
    oppdragId: Int, oppdragsPeriodeListe: List<Oppdragsperiode>, periode: YearMonth
  ): SkattKonteringerRequest {

    val oppdrag = persistenceService.hentOppdrag(oppdragId).get()
    val oppdragsperiode = oppdragsPeriodeListe.first()

    val skattKonteringerRequest = SkattKonteringerRequest(
      listOf(
        SkattKontering(
          Transaksjonskode.A1, //TODO
          Type.NY, //TODO
          null, //TODO
          null, //TODO
          oppdragsperiode.gjelderIdent,
          oppdrag.kravhaverIdent,
          oppdragsperiode.mottakerIdent,
          oppdrag.skyldnerIdent,
          oppdragsperiode.belop,
          oppdragsperiode.valuta,
          periode,
          oppdragsperiode.vedtaksdato,
          LocalDate.now(),
          oppdragsperiode.opprettetAv,
          oppdragsperiode.opprettetAv,
          oppdragsperiode.tekst,
          "Bidrag-regnskap", //TODO
          oppdragsperiode.delytelseId
        )
      )
    )
    return skattKonteringerRequest
  }

  private fun hentOppdragsperioderForPeriode(oppdragId: Int, periode: YearMonth): List<Oppdragsperiode> {
    val oppdragsPeriodeListe = persistenceService.hentOppdragsperiodePaOppdragsIdSomErAktiv(oppdragId)
    return oppdragsPeriodeListe.filter {
      YearMonth.from(it.periodeFra).minusMonths(1).isBefore(periode) &&
          YearMonth.from(it.periodeTil).isAfter(periode) }
  }
}