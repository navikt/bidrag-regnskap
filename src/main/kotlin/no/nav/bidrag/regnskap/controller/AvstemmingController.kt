package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.bidrag.regnskap.service.AvstemmingService
import no.nav.security.token.support.core.api.Protected
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Protected
@Tag(name = "Avstemming")
class AvstemmingController(
    private val avstemmingService: AvstemmingService,
) {

    @OptIn(DelicateCoroutinesApi::class)
    @GetMapping("/avstemming")
    @Operation(
        summary = "Start manuell generering av avstemming- og summeringsfil for dato.",
        description = "Operasjon for å starte generering av avstemmingsfil og summeringsfil for alle konteringer lest inn en spesifikk dato." +
            "Disse filene blir lastet opp i bucket på GCP og deretter overført til en sftp filsluse hvor ELIN plukker ned filene.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Avstemmingsfilene har blitt generert.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Dato er satt frem i tid. Generering blir derfor ikke startet.",
                content = [Content()],
            ),
        ],
    )
    @Parameters(
        value = [
            Parameter(name = "dato", example = "2022-01-01"),
            Parameter(name = "fomTidspunkt", example = "2022-01-01T10:00:00"),
            Parameter(name = "tomTidspunkt", example = "2022-01-01T11:00:00"),
        ],
    )
    fun startAvstemmingsgenerering(
        @RequestParam(required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        dato: LocalDate,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        fomTidspunkt: LocalDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        tomTidspunkt: LocalDateTime?,
    ): ResponseEntity<Any> {
        if (dato.isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().build()
        }
        GlobalScope.launch {
            if (fomTidspunkt != null && tomTidspunkt != null) {
                avstemmingService.startAvstemming(dato, fomTidspunkt, tomTidspunkt)
            } else {
                avstemmingService.startAvstemming(dato)
            }
        }
        return ResponseEntity.ok().build()
    }

    @GetMapping("/manuellOverforingAvstemning")
    @Operation(
        summary = "Start manuell overføring av avstemming- og summeringsfil for dato fra GCP bucket til SFTP.",
        description = "Operasjon for å starte manuell overføring av avstemming-og summeringsfil for en spesifikk dato." +
            "Hentes fra bucket på GCP og deretter overført til en sftp filsluse hvor ELIN plukker ned filene.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Avstemming- og summeringsfil har blitt overført.",
                content = [Content()],
            ),
        ],
    )
    @Parameters(
        value = [
            Parameter(name = "dato", example = "2022-01-01"),
        ],
    )
    fun startManuellOverføringAvstemning(
        @RequestParam(required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        dato: LocalDate,
    ): ResponseEntity<Any> {
        avstemmingService.startManuellOverføringAvsteming(dato)
        return ResponseEntity.ok().build()
    }
}
