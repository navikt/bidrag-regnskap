package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.regnskap.service.VedtakHendelseService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(
  name = "Vedtak hendelse"
)
class VedtakHendelseController(
  private val vedtakHendelseService: VedtakHendelseService
) {

  @PostMapping("/vedtakHendelse")
  @Operation(
    summary = "Manuelt legg inn meldinger fra kafka topic'en bidrag.vedtak",
    description = "Operasjon for å lagre sende inn en kafka melding.",
    security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "200",
      description = "Meldingen er lest vellykket."
    ), ApiResponse(
      responseCode = "400",
      description = "Noe er galt med meldingen.",
      content = [Content()]
    ),ApiResponse(
      responseCode = "401",
      description = "Klienten ikke er autentisert.",
      content = [Content()]
    ), ApiResponse(
      responseCode = "403",
      description = "Klienten ikke har tilgang.",
      content = [Content()]
    )]
  )
  fun opprettHendelse(hendelse: String): ResponseEntity<Any> {
    vedtakHendelseService.behandleHendelse(hendelse)
    return ResponseEntity.ok().build()
  }
}