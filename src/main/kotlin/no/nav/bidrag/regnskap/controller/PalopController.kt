package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.regnskap.dto.PalopRequest
import no.nav.bidrag.regnskap.dto.PalopResponse
import no.nav.bidrag.regnskap.service.PalopService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class PalopController(
  val palopService: PalopService
) {

  @GetMapping("/palop")
  @Operation(
    description = "Operasjon for å hente planlagte og gjennomførte påløpskjøringer.",
    security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "200",
      description = "Returnerer påløp."
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
  fun hentPalop(): ResponseEntity<List<PalopResponse>> {
    return ResponseEntity.ok(palopService.hentPalop())
  }

  @PostMapping("/palop")
  @Operation(
    description = "Operasjon for å lagre planlagte og gjennomførte påløpskjøringer.",
    security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "200",
      description = "Lagret påløp."
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
  fun lagrePalop(palopRequest: PalopRequest): ResponseEntity<Int> {
    return ResponseEntity.ok(palopService.lagrePalop(palopRequest))
  }
}