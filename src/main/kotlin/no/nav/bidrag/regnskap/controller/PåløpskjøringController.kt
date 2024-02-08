package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.regnskap.service.PåløpskjøringService
import no.nav.security.token.support.core.api.Protected
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@Protected
@Tag(name = "Påløpskjøring")
class PåløpskjøringController(
    private val påløpskjøringService: PåløpskjøringService,
) {

    @PostMapping("/palopskjoring")
    @Operation(
        summary = "Start manuel generering av påløpsfil",
        description = "Operasjon for å starte påløpskjøring. Vil starte eldste ikke gjennomførte påløp i 'palop' tabellen. " +
            "Informasjon om påløp kan hentes fra \"/palop\" endepunktet.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Påløpskjøringen har startet. Returnerer ID'en til påløpet.",
            ), ApiResponse(
                responseCode = "204",
                description = "Det finnes ingen ikke gjennomførte påløp.",
                content = [Content()],
            ),
        ],
    )
    fun startPåløpskjøring(@RequestParam(required = true) genererFil: Boolean): ResponseEntity<Int> {
        val påløp = påløpskjøringService.hentPåløp()?.copy(startetTidspunkt = null) ?: return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        påløpskjøringService.startPåløpskjøring(påløp, false, genererFil)
        return ResponseEntity.status(HttpStatus.CREATED).body(påløp.påløpId)
    }

    @GetMapping("/manuellOverforingPåløp")
    @Operation(
        summary = "Start manuell overføring av påløpsfil for dato fra GCP bucket til SFTP.",
        description = "Operasjon for å starte manuell overføring av påløpsfil for en spesifikk dato." +
            "Hentes fra bucket på GCP og deretter overført til en sftp filsluse hvor ELIN plukker ned filene.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Påløpsfil har blitt overført.",
                content = [Content()],
            ),
        ],
    )
    @Parameters(
        value = [
            Parameter(name = "dato", example = "2022-01-01"),
        ],
    )
    fun startManuellOverføringPåløp(
        @RequestParam(required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        dato: LocalDate,
    ): ResponseEntity<Any> {
        påløpskjøringService.startManuellOverføringPåløp(dato)
        return ResponseEntity.ok().build()
    }
}
