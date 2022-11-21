package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.regnskap.dto.påløp.PåløpRequest
import no.nav.bidrag.regnskap.dto.påløp.PåløpResponse
import no.nav.bidrag.regnskap.service.PåløpsService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(name = "Påløp")
class PåløpsController(
  val påløpsService: PåløpsService
) {

  @GetMapping("/palop")
  @Operation(
    summary = "Hent påløp",
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
  fun hentPåløp(): ResponseEntity<List<PåløpResponse>> {
    return ResponseEntity.ok(påløpsService.hentPåløp())
  }

  @PostMapping("/palop")
  @Operation(
    summary = "Lagre nytt påløp",
    description = "Operasjon for å lagre planlagte og gjennomførte påløpskjøringer.",
    security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "200",
      description = "Lagret påløp."
    ), ApiResponse(
      responseCode = "401",
      description = "Klienten ikke er autentisert.",
      content = [Content()]
    ), ApiResponse(
      responseCode = "403",
      description = "Klienten ikke har tilgang.",
      content = [Content()]
    )]
  )
  fun lagrePåløp(påløpRequest: PåløpRequest): ResponseEntity<Int> {
    return ResponseEntity.ok(påløpsService.lagrePåløp(påløpRequest))
  }
}