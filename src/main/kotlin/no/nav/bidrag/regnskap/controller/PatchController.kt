package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.regnskap.dto.patch.OppdaterUtsattTilDatoRequest
import no.nav.bidrag.regnskap.dto.patch.PatchMottakerRequest
import no.nav.bidrag.regnskap.service.OppdragService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
class PatchController(private val oppdragService: OppdragService) {

    @PostMapping("/patchMottaker")
    fun patchMottaker(@RequestBody patchMottakerRequest: PatchMottakerRequest) {
        oppdragService.patchMottaker(patchMottakerRequest.saksnummer, patchMottakerRequest.kravhaver, patchMottakerRequest.mottaker)
    }

    @PostMapping("/oppdaterUtsattTilDato")
    @Operation(
        summary = "Oppdaterer utsatt til dato for et oppdrag.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Oppdatert utsatt til dato for oppdrag. Returnerer oppdragsid.",
                content = [(Content(schema = Schema(implementation = Int::class)))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Fant ikke oppdrag med angitt oppdragsid.",
                content = [Content()],
            ),
        ],
    )
    fun oppdaterUtsattTilDato(@RequestBody oppdaterUtsattTilDatoRequest: OppdaterUtsattTilDatoRequest): ResponseEntity<*> {
        val oppdatertOppdrag = oppdragService.oppdaterUtsattTilDato(oppdaterUtsattTilDatoRequest) ?: return ResponseEntity.notFound().build<Any>()
        return ResponseEntity.ok(oppdatertOppdrag)
    }
}
