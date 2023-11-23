package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.commons.security.maskinporten.MaskinportenClient
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(name = "Maskinporten")
class MaskinportenController(
    val maskinportenClient: MaskinportenClient,
) {

    @Operation(
        summary = "Hent Maskinporten-token med gitte scopes",
        description = "Gyldig Maskinporten-token returneres dersom alle oppgitte scopes finnes i konfigurasjonen til klienten.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Gyldig token returnert",
        ),
        ApiResponse(
            responseCode = "400",
            description = "Noen eller alle oppgitte scopes finnes ikke i konfigurasjonen",
            content = [Content()],
        ),
    )
    @GetMapping(value = ["/token"])
    fun hentToken(scopes: String): ResponseEntity<String> {
        return ResponseEntity.ok(maskinportenClient.hentMaskinportenToken(scopes).parsedString)
    }
}
