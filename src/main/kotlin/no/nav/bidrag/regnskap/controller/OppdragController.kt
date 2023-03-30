package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.regnskap.dto.oppdrag.OppdragResponse
import no.nav.bidrag.regnskap.service.OppdragService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(name = "Oppdrag")
class OppdragController(
    private val oppdragService: OppdragService
) {

    @GetMapping("/oppdrag")
    @Operation(
        summary = "Hent lagret oppdrag",
        description = "Operasjon for å hente lagrede oppdrag med tilhørende oppdragsperioder og konteringer. " +
            "Oppdraget returneres med alle historiske konteringer og oppdragsperioder. " +
            "Dette endepunktet er ment til bruk ved feilsøking.",
        security = [SecurityRequirement(name = "bearer-key")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returnerer oppdragets ID.",
                content = [
                    (
                        Content(
                            array = (ArraySchema(schema = Schema(implementation = OppdragResponse::class)))
                        )
                        )
                ]
            ), ApiResponse(
                responseCode = "204",
                description = "Oppdraget finnes ikke."
            )
        ]
    )
    fun hentOppdrag(oppdragId: Int): ResponseEntity<*> {
        val oppdragResponse = oppdragService.hentOppdrag(oppdragId)
        return if (oppdragResponse != null) {
            ResponseEntity.ok(oppdragService.hentOppdrag(oppdragId))
        } else {
            ResponseEntity.status(HttpStatus.NO_CONTENT)
                .header(HttpHeaders.WARNING, "Det finnes ingen oppdrag med angitt oppdragsId: $oppdragId")
                .build<Any>()
        }
    }
}
