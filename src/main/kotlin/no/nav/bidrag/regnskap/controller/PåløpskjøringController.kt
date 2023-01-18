package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.runBlocking
import no.nav.bidrag.regnskap.service.PåløpskjøringService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(name = "Påløpskjøring")
class PåløpskjøringController(
  private val påløpskjøringService: PåløpskjøringService
) {

  @GetMapping("/palopskjoring")
  @Operation(
    summary = "Start manuel generering av påløpsfil",
    description = "Operasjon for å starte påløpskjøring. Vil starte eldste ikke gjennomførte påløp i 'palop' tabellen. " +
        "Informasjon om påløp kan hentes fra \"/palop\" endepunktet.",
    security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "201",
      description = "Påløpskjøringen har startet. Returnerer ID'en til påløpet."
    ), ApiResponse(
      responseCode = "204",
      description = "Det finnes ingen ikke gjennomførte påløp.",
      content = [Content()]
    )]
  )
  fun startPåløpskjøring(): ResponseEntity<Int> {
    val påløp = påløpskjøringService.hentPåløp() ?: return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    runBlocking { påløpskjøringService.startPåløpskjøring(påløp, false) }
    return ResponseEntity.status(HttpStatus.CREATED).body(påløp.påløpId)
  }

  @GetMapping("/stop_palopskjoring")
  @Operation(
    summary = "Stopper pågående generering av påløpsfil",
    description = "Operasjon for å stoppe påløpskjøring.",
    security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "200",
      description = "Påløpskjøringen er stoppet."
    )]
  )
  fun stopPågåendePåløpskjøring(): ResponseEntity<Any> {
    påløpskjøringService.stoppPågåendePåløpskjøring()
    return ResponseEntity.ok().build()
  }
}