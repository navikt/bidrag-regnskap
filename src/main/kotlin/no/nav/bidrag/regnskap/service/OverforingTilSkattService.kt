package no.nav.bidrag.regnskap.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.dto.GebyrRolle
import no.nav.bidrag.regnskap.dto.Justering
import no.nav.bidrag.regnskap.dto.SkattFeiletKonteringerResponse
import no.nav.bidrag.regnskap.dto.SkattKontering
import no.nav.bidrag.regnskap.dto.SkattKonteringerRequest
import no.nav.bidrag.regnskap.dto.SkattVellykketKonteringResponse
import no.nav.bidrag.regnskap.dto.Transaksjonskode
import no.nav.bidrag.regnskap.dto.Type
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.entity.OverforingKontering
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private val LOGGER = LoggerFactory.getLogger(OverforingTilSkattService::class.java)
private val objectMapper = jacksonObjectMapper()

@Service
class OverforingTilSkattService(
  val skattConsumer: SkattConsumer, val persistenceService: PersistenceService
) {

  fun sendKontering(oppdragId: Int, periode: YearMonth): ResponseEntity<*> {
    LOGGER.info("Starter overføring av kontering til skatt for oppdragId: $oppdragId")

    val oppdrag = persistenceService.hentOppdrag(oppdragId).get()
    val oppdragsperioderMedUsendteKonteringer = hentOppdragsperioderMedUsendteKonteringer(oppdrag)

    if (oppdragsperioderMedUsendteKonteringer.isEmpty()) {
      LOGGER.info("Alle konteringer er allerede overført for oppdrag $oppdragId i periode $periode")
      return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body("Alle konteringer er allerede overført for oppdrag $oppdragId i periode $periode")
    }

    val alleIkkeOverforteKonteringer = finnAlleIkkeOverforteKonteringer(oppdragsperioderMedUsendteKonteringer)
    val skattKonteringerListe = opprettSkattKonteringerRequest(alleIkkeOverforteKonteringer, periode)


    val skattResponse = skattConsumer.sendKontering(SkattKonteringerRequest(skattKonteringerListe))

    when (skattResponse.statusCode) {

      HttpStatus.ACCEPTED -> {
        SECURE_LOGGER.info("Mottok svar fra skatt: \n${skattResponse}")

        val skattVellykketKonteringResponse =
          objectMapper.readValue(skattResponse.body, SkattVellykketKonteringResponse::class.java)
        lagreVellykketOverforingAvKonteringer(
          alleIkkeOverforteKonteringer, skattVellykketKonteringResponse, oppdrag, periode
        )
        return ResponseEntity.ok(skattVellykketKonteringResponse)
      }

      HttpStatus.BAD_REQUEST -> {
        LOGGER.info("En eller flere konteringer har ikke gått gjennom validering. Se secure log for mer informasjon.")
        SECURE_LOGGER.info("En eller flere konteringer har ikke gått gjennom validering, ${skattResponse.body}")

        val skattFeiletKonteringResponse =
          objectMapper.readValue(skattResponse.body, SkattFeiletKonteringerResponse::class.java)
        lagreFeiletOverforingAvKonteringer(alleIkkeOverforteKonteringer, skattFeiletKonteringResponse.toString())
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(skattFeiletKonteringResponse)
      }

      HttpStatus.SERVICE_UNAVAILABLE -> {
        LOGGER.info("Tjenesten hos skatt er slått av. Dette kan skje enten ved innlesing av påløpsfil eller ved andre uventede feil.")

        lagreFeiletOverforingAvKonteringer(alleIkkeOverforteKonteringer, skattResponse.statusCode.toString())
        throw HttpServerErrorException(skattResponse.statusCode)
      }

      HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> {
        LOGGER.info("Bidrag-Regnskap er ikke autorisert eller mangler rettigheter for kallet mot skatt.")

        lagreFeiletOverforingAvKonteringer(alleIkkeOverforteKonteringer, skattResponse.statusCode.toString())
        throw JwtTokenUnauthorizedException()
      }

      else -> {
        throw IllegalStateException("Skatt svarte med uventet statuskode. ${skattResponse.statusCode}")
      }
    }
  }

  private fun lagreVellykketOverforingAvKonteringer(
    alleIkkeOverforteKonteringer: List<Kontering>,
    skattVellykketKonteringResponse: SkattVellykketKonteringResponse,
    oppdrag: Oppdrag,
    periode: YearMonth
  ) {
    alleIkkeOverforteKonteringer.forEach { kontering ->
      val now = LocalDateTime.now()
      kontering.overforingstidspunkt = now

      persistenceService.lagreOverforingKontering(
        opprettOverforingKontering(
          kontering = kontering,
          referanseKode = skattVellykketKonteringResponse.batchUid,
          tidspunkt = now,
          kanal = "REST"
        )
      )
    }
    oppdrag.sistOversendtePeriode = periode.toString()
    persistenceService.lagreOppdrag(oppdrag)
  }

  private fun lagreFeiletOverforingAvKonteringer(
    alleIkkeOverforteKonteringer: List<Kontering>, skattFeiletKonteringerResponse: String
  ) {
    alleIkkeOverforteKonteringer.forEach { kontering ->
      persistenceService.lagreOverforingKontering(
        opprettOverforingKontering(
          kontering = kontering,
          tidspunkt = LocalDateTime.now(),
          feilmelding = skattFeiletKonteringerResponse,
          kanal = "REST"
        )
      )
    }
  }

  private fun opprettOverforingKontering(
    kontering: Kontering,
    referanseKode: String? = null,
    tidspunkt: LocalDateTime,
    feilmelding: String? = null,
    kanal: String
  ): OverforingKontering {
    return OverforingKontering(
      kontering = kontering,
      referansekode = referanseKode,
      feilmelding = feilmelding,
      tidspunkt = tidspunkt,
      kanal = kanal
    )
  }

  private fun opprettSkattKonteringerRequest(
    konteringerListe: List<Kontering>, periode: YearMonth
  ): List<SkattKontering> {
    val skattKonteringerListe = mutableListOf<SkattKontering>()
    konteringerListe.forEach { kontering ->
      skattKonteringerListe.add(
        SkattKontering(
          transaksjonskode = Transaksjonskode.valueOf(kontering.transaksjonskode),
          type = Type.valueOf(kontering.type),
          justering = kontering.justering?.let { Justering.valueOf(kontering.justering) },
          gebyrRolle = kontering.gebyrRolle?.let { GebyrRolle.valueOf(kontering.gebyrRolle) },
          gjelderIdent = kontering.oppdragsperiode!!.gjelderIdent,
          kravhaverIdent = kontering.oppdragsperiode.oppdrag!!.kravhaverIdent,
          mottakerIdent = kontering.oppdragsperiode.mottakerIdent,
          skyldnerIdent = kontering.oppdragsperiode.oppdrag.skyldnerIdent,
          belop = kontering.oppdragsperiode.belop,
          valuta = kontering.oppdragsperiode.valuta,
          periode = periode,
          vedtaksdato = kontering.oppdragsperiode.vedtaksdato,
          kjoredato = LocalDate.now(),
          saksbehandlerId = kontering.oppdragsperiode.opprettetAv,
          attestantId = kontering.oppdragsperiode.opprettetAv,
          tekst = kontering.oppdragsperiode.tekst,
          fagsystemId = "Bidrag-regnskap", //TODO hvordan finne denne?
          delytelsesId = kontering.oppdragsperiode.delytelseId
        )
      )
    }
    return skattKonteringerListe
  }

  private fun hentOppdragsperioderMedUsendteKonteringer(
    oppdrag: Oppdrag
  ): List<Oppdragsperiode> {
    val oppdragsperioder = mutableListOf<Oppdragsperiode>()

    oppdrag.oppdragsperioder?.forEach { oppdragsperiode ->
      if (finnesDetIkkeOverforteKonteringer(oppdragsperiode)) oppdragsperioder.add(oppdragsperiode)
    }
    return oppdragsperioder
  }

  private fun finnesDetIkkeOverforteKonteringer(oppdragsperiode: Oppdragsperiode): Boolean {
    oppdragsperiode.konteringer?.forEach { kontering ->
      if (kontering.overforingstidspunkt == null) {
        return true
      }
    }
    return false
  }

  private fun finnAlleIkkeOverforteKonteringer(oppdragsperioder: List<Oppdragsperiode>): List<Kontering> {
    val konteringer = mutableListOf<Kontering>()

    oppdragsperioder.forEach { oppdragsperiode ->
      konteringer.addAll(oppdragsperiode.konteringer!!.filter { kontering ->
        kontering.overforingstidspunkt == null
      })
    }
    return konteringer
  }
}