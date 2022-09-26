package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.regnskap.dto.OppdragRequest
import no.nav.bidrag.regnskap.dto.OppdragResponse
import no.nav.bidrag.regnskap.service.OppdragService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class OppdragController(
  val oppdragService: OppdragService
) {

  @GetMapping("/oppdrag")
  @Operation(
    description = "Operasjon for å hente lagrede oppdrag med tilhørende oppdragsperioder og konteringer",
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

  @PostMapping("/oppdrag")
  @Operation(
    description = "Operasjon for å lagre oppdrag med tilhørende oppdragsperiode",
    security = [SecurityRequirement(name = "bearer-key")],
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "200",
      description = "Returnerer id for opprettet oppdrag."
    ), ApiResponse(
      responseCode = "400",
      description = "Dersom det er noe galt med requesten"
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
  fun lagreOppdrag(oppdragRequest: OppdragRequest): ResponseEntity<Int> {
    return ResponseEntity.ok(oppdragService.lagreOppdrag(oppdragRequest))
  }
}

