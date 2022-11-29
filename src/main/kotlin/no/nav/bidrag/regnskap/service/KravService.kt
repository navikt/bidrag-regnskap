package no.nav.bidrag.regnskap.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.dto.enumer.SøknadType
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.dto.enumer.Type
import no.nav.bidrag.regnskap.dto.krav.Krav
import no.nav.bidrag.regnskap.dto.krav.KravKontering
import no.nav.bidrag.regnskap.dto.krav.KravResponse
import no.nav.bidrag.regnskap.dto.krav.Kravfeil
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.entity.OverføringKontering
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private val LOGGER = LoggerFactory.getLogger(KravService::class.java)
private val objectMapper = jacksonObjectMapper()

@Service
class KravService(
  val skattConsumer: SkattConsumer, val persistenceService: PersistenceService
) {

  fun sendKrav(oppdragId: Int, periode: YearMonth): ResponseEntity<*> {
    LOGGER.info("Starter overføring av krav til skatt for oppdragId: $oppdragId")

    val oppdrag = persistenceService.hentOppdrag(oppdragId).get()
    val oppdragsperioderMedIkkeOverførteKonteringer = hentOppdragsperioderMedIkkeOverførteKonteringer(oppdrag)

    if (oppdragsperioderMedIkkeOverførteKonteringer.isEmpty()) {
      LOGGER.info("Alle konteringer er allerede overført for oppdrag $oppdragId i periode $periode")
      return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body("Alle konteringer er allerede overført for oppdrag $oppdragId i periode $periode")
    }

    val alleIkkeOverførteKonteringer = finnAlleIkkeOverførteKonteringer(oppdragsperioderMedIkkeOverførteKonteringer)
    val skattKravRequest = opprettSkattKravRequest(alleIkkeOverførteKonteringer)

    val skattResponse = skattConsumer.sendKrav(skattKravRequest)

    when (skattResponse.statusCode) {

      HttpStatus.ACCEPTED -> {
        SECURE_LOGGER.info("Mottok svar fra skatt: \n${skattResponse}")

        val kravResponse = objectMapper.readValue(skattResponse.body, KravResponse::class.java)
        lagreVellykketOverføringAvKrav(alleIkkeOverførteKonteringer, kravResponse, oppdrag, periode)
        return ResponseEntity.ok(kravResponse)
      }

      HttpStatus.BAD_REQUEST -> {
        LOGGER.error("En eller flere konteringer har ikke gått gjennom validering. Se secure log for mer informasjon.")
        SECURE_LOGGER.error("En eller flere konteringer har ikke gått gjennom validering, ${skattResponse.body}")

        val kravfeil = objectMapper.readValue(skattResponse.body, Kravfeil::class.java)
        lagreFeiletOverføringAvKrav(alleIkkeOverførteKonteringer, kravfeil.toString())
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(kravfeil)
      }

      HttpStatus.SERVICE_UNAVAILABLE -> {
        LOGGER.info("Tjenesten hos skatt er slått av. Dette kan skje enten ved innlesing av påløpsfil eller ved andre uventede feil.")

        lagreFeiletOverføringAvKrav(alleIkkeOverførteKonteringer, skattResponse.statusCode.toString())
        throw HttpServerErrorException(skattResponse.statusCode)
      }

      HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> {
        LOGGER.info("Bidrag-Regnskap er ikke autorisert eller mangler rettigheter for kallet mot skatt.")

        lagreFeiletOverføringAvKrav(alleIkkeOverførteKonteringer, skattResponse.statusCode.toString())
        throw JwtTokenUnauthorizedException()
      }

      else -> throw IllegalStateException("Skatt svarte med uventet statuskode. ${skattResponse.statusCode}")
    }
  }

  fun erVedlikeholdsmodusPåslått(): Boolean {
    return skattConsumer.hentStatusPåVedlikeholdsmodus().statusCode == HttpStatus.SERVICE_UNAVAILABLE
  }

  private fun lagreVellykketOverføringAvKrav(
    alleIkkeOverforteKonteringer: List<Kontering>,
    kravResponse: KravResponse,
    oppdrag: Oppdrag,
    periode: YearMonth
  ) {
    alleIkkeOverforteKonteringer.forEach { kontering ->
      val now = LocalDateTime.now()
      kontering.overføringstidspunkt = now

      persistenceService.lagreOverføringKontering(
        opprettOverføringKontering(
          kontering = kontering, referanseKode = kravResponse.batchUid, tidspunkt = now, kanal = "REST"
        )
      )
    }
    oppdrag.sistOversendtePeriode = periode.toString()
    persistenceService.lagreOppdrag(oppdrag)
  }

  private fun lagreFeiletOverføringAvKrav(
    alleIkkeOverforteKonteringer: List<Kontering>, skattFeiletKravResponse: String
  ) {
    alleIkkeOverforteKonteringer.forEach { kontering ->
      persistenceService.lagreOverføringKontering(
        opprettOverføringKontering(
          kontering = kontering, tidspunkt = LocalDateTime.now(), feilmelding = skattFeiletKravResponse, kanal = "REST"
        )
      )
    }
  }

  private fun opprettOverføringKontering(
    kontering: Kontering, referanseKode: String? = null, tidspunkt: LocalDateTime, feilmelding: String? = null, kanal: String
  ): OverføringKontering {
    return OverføringKontering(
      kontering = kontering, referansekode = referanseKode, feilmelding = feilmelding, tidspunkt = tidspunkt, kanal = kanal
    )
  }

  private fun opprettSkattKravRequest(konteringerListe: List<Kontering>): Krav {
    val kravKonteringerListe = mutableListOf<KravKontering>()
    konteringerListe.forEach { kontering ->
      kravKonteringerListe.add(
        KravKontering(
          transaksjonskode = Transaksjonskode.valueOf(kontering.transaksjonskode),
          type = Type.valueOf(kontering.type),
          soknadType = SøknadType.valueOf(kontering.søknadType),
          gjelderIdent = kontering.oppdragsperiode!!.gjelderIdent,
          kravhaverIdent = kontering.oppdragsperiode.oppdrag!!.kravhaverIdent,
          mottakerIdent = kontering.oppdragsperiode.mottakerIdent,
          skyldnerIdent = kontering.oppdragsperiode.oppdrag.skyldnerIdent,
          belop = kontering.oppdragsperiode.beløp,
          valuta = kontering.oppdragsperiode.valuta,
          periode = YearMonth.parse(kontering.overføringsperiode),
          vedtaksdato = kontering.oppdragsperiode.vedtaksdato,
          kjoredato = LocalDate.now(),
          saksbehandlerId = kontering.oppdragsperiode.opprettetAv,
          attestantId = kontering.oppdragsperiode.opprettetAv,
          tekst = kontering.oppdragsperiode.oppdrag.eksternReferanse,
          fagsystemId = kontering.oppdragsperiode.sakId,
          delytelsesId = kontering.oppdragsperiode.delytelseId
        )
      )
    }
    return Krav(kravKonteringerListe)
  }

  private fun hentOppdragsperioderMedIkkeOverførteKonteringer(
    oppdrag: Oppdrag
  ): List<Oppdragsperiode> {
    val oppdragsperioder = mutableListOf<Oppdragsperiode>()

    oppdrag.oppdragsperioder?.forEach { oppdragsperiode ->
      if (finnesDetIkkeOverførteKonteringer(oppdragsperiode)) oppdragsperioder.add(oppdragsperiode)
    }
    return oppdragsperioder
  }

  private fun finnesDetIkkeOverførteKonteringer(oppdragsperiode: Oppdragsperiode): Boolean {
    oppdragsperiode.konteringer?.forEach { kontering ->
      if (kontering.overføringstidspunkt == null) {
        return true
      }
    }
    return false
  }

  private fun finnAlleIkkeOverførteKonteringer(oppdragsperioder: List<Oppdragsperiode>): List<Kontering> {
    val konteringer = mutableListOf<Kontering>()

    oppdragsperioder.forEach { oppdragsperiode ->
      konteringer.addAll(oppdragsperiode.konteringer!!.filter { kontering ->
        kontering.overføringstidspunkt == null
      })
    }
    return konteringer
  }
}