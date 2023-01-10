package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.regnskap.persistence.entity.Driftsavvik
import no.nav.bidrag.regnskap.service.PersistenceService
import no.nav.security.token.support.core.api.Protected
import org.springframework.data.domain.PageRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@Protected
@Tag(name = "Driftsavvik")
class DriftsavvikController(
  private val persistenceService: PersistenceService
) {

  @GetMapping("/aktiveDriftsavvik")
  @Operation(
    summary = "Henter alle aktive driftsavvik",
    security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "200",
      description = "Vedlikeholdsmodus ble endret.",
      content = [Content()]
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
  fun hentAlleAktiveDriftsavvik(): ResponseEntity<List<Driftsavvik>> {
    return ResponseEntity.ok(persistenceService.hentAlleAktiveDriftsavvik())
  }

  @GetMapping("/driftsavvik")
  @Operation(
    summary = "Henter siste driftsavvik",
    security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "200",
      description = "Vedlikeholdsmodus ble endret.",
      content = [Content()]
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
  fun hentDriftsavvik(antallDriftsavvik: Int): ResponseEntity<List<Driftsavvik>> {
    return ResponseEntity.ok(persistenceService.hentDriftsavvik(PageRequest.of(0, antallDriftsavvik)))
  }


  @PostMapping("/driftsavvik")
  @Operation(
    summary = "Oppretter nytt driftsavvik",
    security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "200",
      description = "Vedlikeholdsmodus ble endret.",
      content = [Content()]
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
  @Parameters(value = [
    Parameter(name = "tidspunktFra", example = "2022-01-01T10:00:00"),
    Parameter(name = "tidspunktTil", example = "2022-01-02T10:00:00")
  ])
  fun lagreDriftsavvik(
    @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) tidspunktFra: LocalDateTime,
    @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) tidspunktTil: LocalDateTime,
    @RequestParam(required = false) opprettetAv: String?,
    @RequestParam(required = false) årsak: String?
  ): ResponseEntity<Driftsavvik> {

    return ResponseEntity.ok(
      persistenceService.lagreDriftsavvik(
        Driftsavvik(
          tidspunktFra = tidspunktFra,
          tidspunktTil = tidspunktTil,
          opprettetAv = opprettetAv,
          årsak = årsak
        )
      )
    )
  }
}