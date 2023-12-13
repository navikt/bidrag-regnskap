package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.regnskap.hendelse.kafka.vedtak.VedtakshendelseListener
import no.nav.bidrag.regnskap.service.VedtakshendelseService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(
    name = "Vedtak hendelse",
)
class VedtakshendelseController(
    private val vedtakshendelseService: VedtakshendelseService,
    private val vedtakshendelseListener: VedtakshendelseListener,
) {

    @PostMapping("/vedtakHendelse")
    @Operation(
        summary = "Manuelt legg inn meldinger fra kafka topic'en bidrag.vedtak",
        description = "Operasjon for å lagre sende inn en kafka melding.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Meldingen er lest vellykket.",
                content = [Content()],
            ), ApiResponse(
                responseCode = "400",
                description = "Noe er galt med meldingen.",
                content = [Content()],
            ),
        ],
    )
    fun opprettHendelse(vedtakHendelse: String): ResponseEntity<Any> {
        vedtakshendelseService.behandleHendelse(vedtakHendelse)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/hoppOverNesteVedtakhendselse")
    @Operation(
        summary = "Hopper over neste offset som er lagt inn på kafka-topic.",
        description = "Dette endepunktet må kun brukes om man er helt sikker på at neste offset skal hoppes over.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Offset er hoppet over.",
                content = [Content()],
            ), ApiResponse(
                responseCode = "400",
                description = "Noe er galt med meldingen.",
                content = [Content()],
            ),
        ],
    )
    fun hoppOverNesteMelding(): ResponseEntity<Any> {
        vedtakshendelseListener.hoppOverNesteMelding()
        return ResponseEntity.ok().build()
    }

    @PostMapping("/hoppOverAlleVedtakhendselser")
    @Operation(
        summary = "Hopper over ALLE offset som er lagt inn på kafka-topic frem til siste offset.",
        description = "Dette endepunktet må kun brukes om man er helt sikker på at ALLE offset skal hoppes over.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Alle offsets er hoppet over.",
                content = [Content()],
            ), ApiResponse(
                responseCode = "400",
                description = "Noe er galt med meldingen.",
                content = [Content()],
            ),
        ],
    )
    fun hoppOverAlleMeldinger(): ResponseEntity<Any> {
        vedtakshendelseListener.hoppOverAlleMeldinger()
        return ResponseEntity.ok().build()
    }

    @GetMapping("/sisteLesteHendelse")
    @Operation(
        summary = "Henter informasjon om siste innleste hendelse.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content = [Content()],
            ),
        ],
    )
    fun hentSisteLesteHendelse(): ResponseEntity<String> {
        return ResponseEntity.ok("Siste leste offset er: ${vedtakshendelseListener.hentSisteLesteHendelse()}.")
    }
}
