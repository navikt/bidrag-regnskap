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
    private val persistenceService: PersistenceService,
    private val behandlingsstatusService: BehandlingsstatusService
) {

    @Transactional(
        noRollbackFor = [HttpClientErrorException::class, HttpServerErrorException::class, JwtTokenUnauthorizedException::class],
        propagation = Propagation.REQUIRES_NEW
    )
    fun sendKrav(oppdragIdListe: List<Int>) {
        val oppdragListe = oppdragIdListe.mapNotNull { persistenceService.hentOppdrag(it) }.toMutableList()

        // Fjerner alle oppdrag som har utsatt oversending
        oppdragListe.removeIf { oppdrag ->
            val skalFjerne = oppdrag.utsattTilDato?.isAfter(LocalDate.now()) == true
            if (skalFjerne) {
                LOGGER.info("Oppdrag ${oppdrag.oppdragId} skal ikke oversendes før ${oppdrag.utsattTilDato}. Avventer oversending av krav.")
            }
            skalFjerne
        }

        // Om det finnes ikke godkjente overføringer som er forsøkt overført tidligere så skal det forsøkes å overføres en gang til og om det feiler avbrytes oversending
        oppdragListe.forEach { oppdrag ->
            if (harOppdragFeiledeOverføringer(oppdrag)) {
                val feiledeOverføringer: HashMap<String, String>
                try {
                    feiledeOverføringer =
                        behandlingsstatusService.hentBehandlingsstatusForIkkeGodkjenteKonteringerForReferansekode(
                            hentSisteReferansekoder(oppdrag)
                        )
                } catch (e: Exception) {
                    LOGGER.error("Noe gikk galt ved kall mot behandlingsstatus! ${e.stackTrace}")
                    return
                }

                if (feiledeOverføringer.isNotEmpty()) {
                    val feilmeldingSammenslått = feiledeOverføringer.entries.joinToString("\n") { it.value }
                    LOGGER.error("Det har oppstått feil ved overføring av krav for oppdrag ${oppdrag.oppdragId} på følgende batchUider med følgende feilmelding:\n $feilmeldingSammenslått")
                    return
                }
            }
        }

        if (oppdragListe.isEmpty()) {
            LOGGER.info("Det finnes ingen oppdrag med angitte oppdragsIder: $oppdragIdListe som skal oversendes.")
            return
        }

        val oppdragsperioderMedIkkeOverførteKonteringerListe =
            oppdragListe.flatMap { hentOppdragsperioderMedIkkeOverførteKonteringer(it) }

        if (oppdragsperioderMedIkkeOverførteKonteringerListe.isEmpty()) {
            LOGGER.info("Alle konteringer er allerede overført for alle oppdrag $oppdragIdListe.")
            return
        }

        try {
            val skattResponse =
                skattConsumer.sendKrav(opprettKravliste(oppdragsperioderMedIkkeOverførteKonteringerListe))
            lagreOverføringAvKrav(
                skattResponse,
                finnAlleIkkeOverførteKonteringer(oppdragsperioderMedIkkeOverførteKonteringerListe),
                oppdragListe
            )
        } catch (e: Exception) {
            LOGGER.error("Kallet mot skatt feilet på noe uventet! Feil: ${e.message}, stacktrace: ${e.stackTraceToString()}")
        }

        LOGGER.info("Overføring til skatt fullført for oppdrag: $oppdragIdListe")
    }

    private fun hentSisteReferansekoder(oppdrag: Oppdrag) =
        oppdrag.oppdragsperioder.flatMap { oppdragsperiode ->
            oppdragsperiode.konteringer.flatMap { kontering ->
                listOfNotNull(kontering.sisteReferansekode)
            }
        }.distinct()

    private fun harOppdragFeiledeOverføringer(oppdrag: Oppdrag) =
        oppdrag.oppdragsperioder.any { oppdragsperiode ->
            oppdragsperiode.konteringer.any { kontering ->
                kontering.behandlingsstatusOkTidspunkt == null && kontering.sisteReferansekode != null
            }
        }

    fun opprettKravliste(oppdragsperioderMedIkkeOverførteKonteringerListe: List<Oppdragsperiode>): Kravliste {
        // Gruperer alle oppdragene på vedtakId for å sende over oppdrag knyttet til en vedtakId om gangen,
        // sorterer på vedtakId slik at tidligste vedtak kommer først
        // mapper så til kontering for å opprette en KravKontering per kontering
        return Kravliste(
            finnAlleIkkeOverførteKonteringer(oppdragsperioderMedIkkeOverførteKonteringerListe)
                .groupBy { it.vedtakId }
                .mapValues { entry ->
                    entry.value.sortedBy { kontering -> kontering.vedtakId }
                }.toSortedMap()
                .map { opprettKravKonteringListe(it.value) }
        )
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
                OverføringKontering(
                    kontering = kontering,
                    referansekode = kravResponse.batchUid,
                    tidspunkt = now,
                    kanal = "REST"
                )
            )
        }
        persistenceService.lagreOppdrag(oppdrag)
    }

    private fun lagreFeiletOverføringAvKrav(
        alleIkkeOverforteKonteringer: List<Kontering>,
        skattFeiletKravResponse: String
    ) {
        alleIkkeOverforteKonteringer.forEach { kontering ->
            persistenceService.lagreOverføringKontering(
                OverføringKontering(
                    kontering = kontering,
                    tidspunkt = LocalDateTime.now(),
                    feilmelding = skattFeiletKravResponse,
                    kanal = "REST"
                )
            )
        }
    }

    fun opprettKravKonteringListe(konteringerListe: List<Kontering>): Krav {
        return Krav(
            konteringerListe.map { kontering ->
                Kravkontering(
                    transaksjonskode = Transaksjonskode.valueOf(kontering.transaksjonskode),
                    type = Type.valueOf(kontering.type),
                    soknadType = Søknadstype.valueOf(kontering.søknadType),
                    gjelderIdent = kontering.oppdragsperiode!!.oppdrag!!.gjelderIdent,
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
                    tekst = kontering.oppdragsperiode.eksternReferanse,
                    fagsystemId = kontering.oppdragsperiode.oppdrag.sakId,
                    delytelsesId = kontering.oppdragsperiode.delytelseId.toString()
                )
            }
        )
    }

    fun hentOppdragsperioderMedIkkeOverførteKonteringer(
        oppdrag: Oppdrag
    ): List<Oppdragsperiode> {
        return oppdrag.oppdragsperioder.filter { finnesDetIkkeOverførteKonteringer(it) }
    }

    private fun finnesDetIkkeOverførteKonteringer(oppdragsperiode: Oppdragsperiode): Boolean {
        return oppdragsperiode.konteringer.any { it.overføringstidspunkt == null }
    }

    private fun finnAlleIkkeOverførteKonteringer(oppdragsperioder: List<Oppdragsperiode>): List<Kontering> {
        return oppdragsperioder.flatMap { oppdragsperiode ->
            oppdragsperiode.konteringer.filter { kontering ->
                kontering.overføringstidspunkt == null
            }
        }
    }
}
