package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.domene.enums.regnskap.Årsakskode
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.transport.regnskap.vedlikeholdsmodus.Vedlikeholdsmodus
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(
    name = "Vedlikeholdsmodus",
)
class VedlikeholdsmodusController(
    private val skattConsumer: SkattConsumer,
) {

    @PostMapping("/vedlikeholdsmodus")
    @Operation(
        summary = "Manuelt endrer status på vedlikeholdsdmodus",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Vedlikeholdsmodus ble endret.",
                content = [Content()],
            ),
        ],
    )
    @Parameter(name = "kommentar", example = "Påløp for 2022-12 genereres hos NAV.")
    fun endreVedlikeholdsmodus(aktiv: Boolean, årsakskode: Årsakskode, kommentar: String): ResponseEntity<Any> {
        return skattConsumer.oppdaterVedlikeholdsmodus(Vedlikeholdsmodus(aktiv, årsakskode, kommentar))
    }

    @GetMapping("/vedlikeholdsmodus")
    @Operation(
        summary = "Sjekker status på vedlikeholdsmodus",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Vedlikeholdsmodus er avslått.",
                content = [Content()],
            ), ApiResponse(
                responseCode = "503",
                description = "Vedlikeholdsmodus er påslått.",
                content = [
                    Content(),
                ],
            ),
        ],
    )
    fun sjekkStatusPåVedlikeholdsmodus(): ResponseEntity<Any> {
        return skattConsumer.hentStatusPåVedlikeholdsmodus()
    }
}
