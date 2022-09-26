package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.regnskap.dto.KRAV_BESKRIVELSE
import no.nav.bidrag.regnskap.dto.SkattKonteringerResponse
import no.nav.bidrag.regnskap.service.KonteringService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@Protected
class KonteringController(
  var konteringService: KonteringService
) {

  @PostMapping("/sendKonteringTilSkatt")
  @Operation(
    description = KRAV_BESKRIVELSE, security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "200",
      description = "Alle konteringene i kravet er oppdatert OK. Responsen har tom body.\n\n" +
          "Det forventes også responskode 200 dersom kravet (og dermed konteringene) er overført tidligere. " +
          "Det forventes da at kravet ignoreres slik at ikke konteringene posteres dobbelt.",
      content = [Content()]
    ), ApiResponse(
      responseCode = "400",
      description = "Dersom én av konteringene ikke går gjennom validering forkastes alle konteringene i kravet " +
          "og en liste over konteringer som har feilet returneres, sammen med informasjon om hva som er feil.\n\n" +
          "Det er ingen garanti for at konteringer som ikke kommer med på listen over feilede konteringer er feilfrie.",
      content = [Content(
        mediaType = "application/json",
        array = ArraySchema(schema = Schema(implementation = SkattKonteringerResponse::class))
      )]
    ), ApiResponse(
      responseCode = "401",
      description = "Dersom klienten ikke er autentisert.",
      content = [Content()]
    ), ApiResponse(
      responseCode = "403", description = "Dersom klienten ikke har tilgang.", content = [Content()]
    ), ApiResponse(
      responseCode = "404", description = "Oppdraget finnes ikke.", content = [Content()]
    )]
  )
  @Parameters(
    value = [Parameter(
      name = "oppdragId",
      required = true,
      example = "1"
    ), Parameter(
      name = "periode",
      required = true,
      example = "2022-01"
    )]
  )
  fun sendKonteringer(@RequestParam oppdragId: Int, @RequestParam periode: YearMonth): ResponseEntity<SkattKonteringerResponse> {
    return konteringService.sendKontering(oppdragId, periode)
  }
}

