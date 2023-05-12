package no.nav.bidrag.regnskap.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.dto.enumer.Søknadstype
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.dto.enumer.Type
import no.nav.bidrag.regnskap.dto.krav.Krav
import no.nav.bidrag.regnskap.dto.krav.KravResponse
import no.nav.bidrag.regnskap.dto.krav.Kravfeil
import no.nav.bidrag.regnskap.dto.krav.Kravkontering
import no.nav.bidrag.regnskap.dto.krav.Kravliste
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.entity.OverføringKontering
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import java.time.LocalDate
import java.time.LocalDateTime

private val LOGGER = LoggerFactory.getLogger(KravService::class.java)
private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

@Service
class KravService(
    private val skattConsumer: SkattConsumer,
    private val persistenceService: PersistenceService
) {

    @Transactional(
        noRollbackFor = [HttpClientErrorException::class, HttpServerErrorException::class, JwtTokenUnauthorizedException::class],
        propagation = Propagation.REQUIRES_NEW
    )
    fun sendKrav(oppdragIdListe: List<Int>) {
        LOGGER.info("Starter overføring av krav til skatt for oppdrag: $oppdragIdListe")

        val oppdragListe = oppdragIdListe.mapNotNull { persistenceService.hentOppdrag(it) }

        if (oppdragListe.isEmpty()) {
            LOGGER.error("Det finnes ingen oppdrag med angitte oppdragsIder: $oppdragIdListe")
            return
        }

        // Alle oppdragId i listen skal stamme fra samme vedtak. Disse skal ha samme utsattTilDato.
        if (oppdragListe.first().utsattTilDato?.isAfter(LocalDate.now()) == true) {
            LOGGER.info("Oppdrag $oppdragIdListe skal ikke oversendes før ${oppdragListe.first().utsattTilDato}. Avventer oversending av krav.")
            return
        }

        val oppdragsperioderMedIkkeOverførteKonteringerListe = oppdragListe.flatMap { hentOppdragsperioderMedIkkeOverførteKonteringer(it) }

        if (oppdragsperioderMedIkkeOverførteKonteringerListe.isEmpty()) {
            LOGGER.info("Alle konteringer er allerede overført for alle oppdrag $oppdragIdListe.")
            return
        }

        val alleIkkeOverførteKonteringer = finnAlleIkkeOverførteKonteringer(oppdragsperioderMedIkkeOverførteKonteringerListe)

        try {
            val skattResponse = skattConsumer.sendKrav(opprettSkattKravRequest(alleIkkeOverførteKonteringer))
            lagreOverføringAvKrav(skattResponse, alleIkkeOverførteKonteringer, oppdragListe)
        } catch (e: Exception) {
            LOGGER.error("Kallet mot skatt feilet på noe uventet! Feil: ${e.message}, stacktrace: ${e.stackTraceToString()}")
        }

        LOGGER.info("Overføring til skatt fullført for oppdrag: $oppdragIdListe")
    }

    private fun lagreOverføringAvKrav(
        skattResponse: ResponseEntity<String>,
        alleIkkeOverførteKonteringer: List<Kontering>,
        oppdrag: List<Oppdrag>
    ) {
        try {
            when (skattResponse.statusCode) {
                HttpStatus.ACCEPTED -> {
                    SECURE_LOGGER.info("Mottok svar fra skatt: \n$skattResponse")
                    val kravResponse = objectMapper.readValue(skattResponse.body, KravResponse::class.java)
                    lagreVellykketOverføringAvKrav(alleIkkeOverførteKonteringer, kravResponse, oppdrag)
                }

                HttpStatus.BAD_REQUEST -> {
                    LOGGER.error("En eller flere konteringer har ikke gått gjennom validering. Se secure log for mer informasjon.")
                    SECURE_LOGGER.error("En eller flere konteringer har ikke gått gjennom validering, ${skattResponse.body}")
                    val kravfeil = objectMapper.readValue(skattResponse.body, Kravfeil::class.java)
                    lagreFeiletOverføringAvKrav(alleIkkeOverførteKonteringer, kravfeil.toString())
                }

                HttpStatus.SERVICE_UNAVAILABLE -> {
                    LOGGER.error(
                        "Skatt svarte med uventet statuskode: ${skattResponse.statusCode}. " +
                            "Tjenesten hos skatt er slått av. Dette kan skje enten ved innlesing av påløpsfil eller ved andre uventede feil. " +
                            "Feilmelding: ${skattResponse.body}"
                    )
                    lagreFeiletOverføringAvKrav(alleIkkeOverførteKonteringer, skattResponse.statusCode.toString())
                }

                HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> {
                    LOGGER.error(
                        "Skatt svarte med uventet statuskode: ${skattResponse.statusCode}. " +
                            "Bidrag-Regnskap er ikke autorisert eller mangler rettigheter for kallet mot skatt. Feilmelding: ${skattResponse.body}"
                    )
                    lagreFeiletOverføringAvKrav(alleIkkeOverførteKonteringer, skattResponse.statusCode.toString())
                }

                else -> {
                    LOGGER.error("Skatt svarte med uventet statuskode: ${skattResponse.statusCode}. Feilmelding: ${skattResponse.body}")
                    lagreFeiletOverføringAvKrav(
                        alleIkkeOverførteKonteringer,
                        "Statuskode: ${skattResponse.statusCode}" + ", body: " + (
                            skattResponse.body
                                ?: "{}"
                            )
                    )
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Tolkningen av svaret fra skatt feilet på noe uventet! Feil: ${e.message}")
            lagreFeiletOverføringAvKrav(
                alleIkkeOverførteKonteringer,
                e.message
                    ?: "Kallet mot skatt feilet på noe uventet! Stackstrace: ${e.stackTraceToString()}"
            )
        }
    }

    fun erVedlikeholdsmodusPåslått(): Boolean {
        return skattConsumer.hentStatusPåVedlikeholdsmodus().statusCode == HttpStatus.SERVICE_UNAVAILABLE
    }

    private fun lagreVellykketOverføringAvKrav(
        alleIkkeOverforteKonteringer: List<Kontering>,
        kravResponse: KravResponse,
        oppdrag: List<Oppdrag>
    ) {
        alleIkkeOverforteKonteringer.forEach { kontering ->
            val now = LocalDateTime.now()
            kontering.overføringstidspunkt = now
            kontering.sisteReferansekode = kravResponse.batchUid

            persistenceService.lagreOverføringKontering(
                opprettOverføringKontering(
                    kontering = kontering,
                    referanseKode = kravResponse.batchUid,
                    tidspunkt = now,
                    kanal = "REST"
                )
            )
        }
        oppdrag.forEach { persistenceService.lagreOppdrag(it) }
    }

    private fun lagreFeiletOverføringAvKrav(
        alleIkkeOverforteKonteringer: List<Kontering>,
        skattFeiletKravResponse: String
    ) {
        alleIkkeOverforteKonteringer.forEach { kontering ->
            persistenceService.lagreOverføringKontering(
                opprettOverføringKontering(
                    kontering = kontering,
                    tidspunkt = LocalDateTime.now(),
                    feilmelding = skattFeiletKravResponse,
                    kanal = "REST"
                )
            )
        }
    }

    private fun opprettOverføringKontering(
        kontering: Kontering,
        referanseKode: String? = null,
        tidspunkt: LocalDateTime,
        feilmelding: String? = null,
        kanal: String
    ): OverføringKontering {
        return OverføringKontering(
            kontering = kontering,
            referansekode = referanseKode,
            feilmelding = feilmelding,
            tidspunkt = tidspunkt,
            kanal = kanal
        )
    }

    fun opprettSkattKravRequest(konteringerListe: List<Kontering>): Kravliste {
        val kravKonteringerListe = mutableListOf<Kravkontering>()
        konteringerListe.forEach { kontering ->
            kravKonteringerListe.add(
                Kravkontering(
                    transaksjonskode = Transaksjonskode.valueOf(kontering.transaksjonskode),
                    type = Type.valueOf(kontering.type),
                    soknadType = Søknadstype.valueOf(kontering.søknadType),
                    gjelderIdent = kontering.oppdragsperiode!!.gjelderIdent,
                    kravhaverIdent = kontering.oppdragsperiode.oppdrag!!.kravhaverIdent,
                    mottakerIdent = kontering.oppdragsperiode.mottakerIdent,
                    skyldnerIdent = kontering.oppdragsperiode.oppdrag.skyldnerIdent,
                    belop = if (Transaksjonskode.valueOf(kontering.transaksjonskode).negativtBeløp) kontering.oppdragsperiode.beløp.negate() else kontering.oppdragsperiode.beløp,
                    valuta = kontering.oppdragsperiode.valuta,
                    periode = kontering.overføringsperiode,
                    vedtaksdato = kontering.oppdragsperiode.vedtaksdato.toString(),
                    kjoredato = LocalDate.now().toString(),
                    saksbehandlerId = kontering.oppdragsperiode.opprettetAv,
                    attestantId = kontering.oppdragsperiode.opprettetAv,
                    eksternReferanse = kontering.oppdragsperiode.eksternReferanse,
                    fagsystemId = kontering.oppdragsperiode.oppdrag.sakId,
                    delytelsesId = kontering.oppdragsperiode.delytelseId.toString()
                )
            )
        }
        return Kravliste(listOf(Krav(kravKonteringerListe)))
    }

    private fun hentOppdragsperioderMedIkkeOverførteKonteringer(
        oppdrag: Oppdrag
    ): List<Oppdragsperiode> {
        val oppdragsperioder = mutableListOf<Oppdragsperiode>()

        oppdrag.oppdragsperioder.forEach { oppdragsperiode ->
            if (finnesDetIkkeOverførteKonteringer(oppdragsperiode)) oppdragsperioder.add(oppdragsperiode)
        }
        return oppdragsperioder
    }

    private fun finnesDetIkkeOverførteKonteringer(oppdragsperiode: Oppdragsperiode): Boolean {
        oppdragsperiode.konteringer.forEach { kontering ->
            if (kontering.overføringstidspunkt == null) {
                return true
            }
        }
        return false
    }

    private fun finnAlleIkkeOverførteKonteringer(oppdragsperioder: List<Oppdragsperiode>): List<Kontering> {
        val konteringer = mutableListOf<Kontering>()

        oppdragsperioder.forEach { oppdragsperiode ->
            konteringer.addAll(
                oppdragsperiode.konteringer.filter { kontering ->
                    kontering.overføringstidspunkt == null
                }
            )
        }
        return konteringer
    }
}
