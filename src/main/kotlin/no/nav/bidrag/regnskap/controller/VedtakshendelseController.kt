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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(
    name = "Vedtak hendelse"
)
class VedtakshendelseController(
    private val vedtakshendelseService: VedtakshendelseService,
    private val vedtakshendelseListener: VedtakshendelseListener
) {

    @PostMapping("/vedtakHendelse")
    @Operation(
        summary = "Manuelt legg inn meldinger fra kafka topic'en bidrag.vedtak",
        description = "Operasjon for å lagre sende inn en kafka melding.",
        security = [SecurityRequirement(name = "bearer-key")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Meldingen er lest vellykket.",
                content = [Content()]
            ), ApiResponse(
                responseCode = "400",
                description = "Noe er galt med meldingen.",
                content = [Content()]
            )
        ]
    )
    fun opprettHendelse(vedtakHendelse: String): ResponseEntity<Any> {
        vedtakshendelseService.behandleHendelse(vedtakHendelse)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/hoppOverVedtakhendselse")
    @Operation(
        summary = "Hopper over neste offset som er lagt inn på kafka-topic.",
        description = "Dette endepunktet må kun brukes om man er helt sikker på at neste offset skal hoppes over.",
        security = [SecurityRequirement(name = "bearer-key")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Offset er hoppet over.",
                content = [Content()]
            ), ApiResponse(
                responseCode = "400",
                description = "Noe er galt med meldingen.",
                content = [Content()]
            )
        ]
    )
    fun hoppOverMelding(): ResponseEntity<Any> {
        vedtakshendelseListener.hoppOverNesteMelding()
        return ResponseEntity.ok().build()
    }
}
