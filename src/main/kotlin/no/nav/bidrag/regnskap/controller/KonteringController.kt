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
import no.nav.bidrag.regnskap.dto.SKATT_SENDT_KONTERING_BESKRIVELSE
import no.nav.bidrag.regnskap.dto.SkattFeiletKonteringerResponse
import no.nav.bidrag.regnskap.dto.SkattVellykketKonteringResponse
import no.nav.bidrag.regnskap.service.OverforingTilSkattService
import no.nav.bidrag.regnskap.service.PersistenceService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@Protected
class KonteringController(
  val overforingTilSkattService: OverforingTilSkattService,
  val persistenceService: PersistenceService
) {

  @PostMapping("/sendKonteringTilSkatt")
  @Operation(
    description = SKATT_SENDT_KONTERING_BESKRIVELSE, security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [ApiResponse(
      responseCode = "200",
      description = "Alle konteringene i kravet er oppdatert OK. " +
          "Responsen inneholder BatchUid som er en referanse til batch kjøringen hos ELIN.\n\n" +
          "Det forventes også responskode 200 dersom kravet (og dermed konteringene) er overført tidligere. " +
          "Det forventes da at kravet ignoreres slik at ikke konteringene posteres dobbelt.",
      content = [Content(
        mediaType = "application/json",
        array = ArraySchema(schema = Schema(implementation = SkattVellykketKonteringResponse::class))
      )]
    ), ApiResponse(
      responseCode = "204",
      description = "Dersom alle konteringer i kravet allerede er overført.",
      content = [Content(
        mediaType = "application/json",
        array = ArraySchema(schema = Schema(implementation = String::class))
      )]
    ), ApiResponse(
      responseCode = "400",
      description = "Dersom én av konteringene ikke går gjennom validering forkastes alle konteringene i kravet " +
          "og en liste over konteringer som har feilet returneres, sammen med informasjon om hva som er feil.\n\n" +
          "Det er ingen garanti for at konteringer som ikke kommer med på listen over feilede konteringer er feilfrie.",
      content = [Content(
        mediaType = "application/json",
        array = ArraySchema(schema = Schema(implementation = SkattFeiletKonteringerResponse::class))
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
  fun sendKonteringer(@RequestParam oppdragId: Int, @RequestParam periode: YearMonth): ResponseEntity<*> {
    val sisteOverfortePeriode = persistenceService.finnSisteOverfortePeriode()
    if (sisteOverfortePeriode.isBefore(periode)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Påløpsfil er ikke kjørt for $periode. Siste kjørte påløpsperiode er $sisteOverfortePeriode")
    }
    return overforingTilSkattService.sendKontering(oppdragId, periode)
  }
}

