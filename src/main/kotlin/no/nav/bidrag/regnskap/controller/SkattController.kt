package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.regnskap.model.KRAV_BESKRIVELSE
import no.nav.bidrag.regnskap.model.KravRequest
import no.nav.bidrag.regnskap.model.KravResponse
import no.nav.bidrag.regnskap.service.KravService
import no.nav.security.token.support.core.api.Protected
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
    description = KRAV_BESKRIVELSE,
    security = [SecurityRequirement(name = "bearer-key")]
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Alle konteringer i kravet er oppdatert OK"),
      ApiResponse(responseCode = "400", description = "Noe gikk feil under validering av konteringene i kravet"),
      ApiResponse(responseCode = "401", description = "Sikkerhetstokenet er ikke gyldig"),
      ApiResponse(responseCode = "403", description = "Mangler tilgang for å lagre krav")
    ]
  )
  @ResponseBody
  fun lagreKrav(@RequestBody kravRequest: KravRequest): KravResponse? {
    return kravService.lagreKrav(kravRequest)
  }

}

