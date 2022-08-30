package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.regnskap.model.KRAV_BESKRIVELSE
import no.nav.bidrag.regnskap.model.KravRequest
import no.nav.bidrag.regnskap.model.KravResponse
import no.nav.bidrag.regnskap.service.KravService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class SkattController(
  var kravService: KravService
) {

  @PostMapping("/api/krav")
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
        array = ArraySchema(schema = Schema(implementation = KravResponse::class))
      )]
    ), ApiResponse(
      responseCode = "401",
      description = "Dersom klienten ikke er autentisert.",
      content = [Content()]
    ), ApiResponse(
      responseCode = "403", description = "Dersom klienten ikke har tilgang.", content = [Content()]
    )]
  )
  @ResponseBody
  fun lagreKrav(@RequestBody kravRequest: KravRequest): ResponseEntity<KravResponse> {
    return kravService.lagreKrav(kravRequest)
  }
}

