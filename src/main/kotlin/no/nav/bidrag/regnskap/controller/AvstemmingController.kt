package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.regnskap.service.AvstemmingService
import no.nav.security.token.support.core.api.Protected
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate


@RestController
@Protected
@Tag(name = "Avstemming")
class AvstemmingController(
  private val avstemmingService: AvstemmingService
) {

  @GetMapping("/avstemming")
  @Operation(
    summary = "Start manuell generering av avstemming- og summeringsfil for dato.",
    description = "Operasjon for å starte generering av avstemmingsfil og summeringsfil for alle konteringer lest inn en spesifikk dato." +
        "Disse filene blir lastet opp i bucket på GCP og deretter overført til en sftp filsluse hvor ELIN plukker ned filene.",
    security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "200",
      description = "Avstemmingsfilene har blitt generert.",
      content = [Content()]
    ), ApiResponse(
      responseCode = "400",
      description = "Dato er satt frem i tid. Generering blir derfor ikke startet.",
      content = [Content()]
    )]
  )
  fun startAvstemmingsgenerering(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    dato: LocalDate
  ): ResponseEntity<Any> {
    if (dato.isAfter(LocalDate.now())) {
      return ResponseEntity.badRequest().build()
    }
    avstemmingService.startAvstemming(dato)
    return ResponseEntity.ok().build()
  }

}