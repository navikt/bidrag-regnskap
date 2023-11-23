package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.regnskap.dto.påløp.PåløpRequest
import no.nav.bidrag.regnskap.dto.påløp.PåløpResponse
import no.nav.bidrag.regnskap.service.PåløpsService
import no.nav.security.token.support.core.api.Protected
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.YearMonth

@RestController
@Protected
@Tag(name = "Påløp")
class PåløpsController(
    val påløpsService: PåløpsService,
) {

    @GetMapping("/palop")
    @Operation(
        summary = "Hent påløp",
        description = "Operasjon for å hente planlagte og gjennomførte påløpskjøringer.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returnerer påløp.",
            ),
        ],
    )
    fun hentPåløp(): ResponseEntity<List<PåløpResponse>> {
        return ResponseEntity.ok(påløpsService.hentPåløp())
    }

    @PostMapping("/palop")
    @Operation(
        summary = "Lagre nytt påløp",
        description = "Operasjon for å lagre planlagte og gjennomførte påløpskjøringer.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Lagret påløp.",
            ),
        ],
    )
    @Parameters(
        value = [
            Parameter(name = "kjøredato", example = "2022-01-01T10:00:00"),
            Parameter(name = "forPeriode", example = "2022-02"),
        ],
    )
    fun lagrePåløp(
        @RequestParam(required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        kjøredato: LocalDateTime,
        @RequestParam(required = true) forPeriode: String,
    ): ResponseEntity<Int> {
        return ResponseEntity.ok(påløpsService.lagrePåløp(PåløpRequest(kjøredato, YearMonth.parse(forPeriode))))
    }
}
