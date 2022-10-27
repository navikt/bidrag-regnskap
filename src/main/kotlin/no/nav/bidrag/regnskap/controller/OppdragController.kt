package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.regnskap.dto.OppdragResponse
import no.nav.bidrag.regnskap.service.OppdragService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class OppdragController(
  val oppdragService: OppdragService
) {

  @GetMapping("/oppdrag")
  @Operation(
    description = "Operasjon for å hente lagrede oppdrag med tilhørende oppdragsperioder og konteringer. " +
        "Oppdraget returneres med alle historiske konteringer og oppdragsperioder. " +
        "Dette endepunktet er ment til bruk ved feilsøking.",
    security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "200",
      description = "Returnerer oppdraget.",
    ), ApiResponse(
      responseCode = "204",
      description = "Dersom oppdraget ikke finnes.",
      content = [Content()]
    ), ApiResponse(
      responseCode = "401",
      description = "Dersom klienten ikke er autentisert.",
      content = [Content()]
    ), ApiResponse(
      responseCode = "403",
      description = "Dersom klienten ikke har tilgang.",
      content = [Content()]
    )]
  )
  fun hentOppdrag(oppdragId: Int): ResponseEntity<OppdragResponse> {
    return ResponseEntity.ok(oppdragService.hentOppdrag(oppdragId))
  }
}

