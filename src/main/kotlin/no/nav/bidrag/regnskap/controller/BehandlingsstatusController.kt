package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.hendelse.schedule.krav.SjekkAvBehandlingsstatusScheduler
import no.nav.bidrag.transport.regnskap.behandlingsstatus.BehandlingsstatusResponse
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(name = "Behandlingsstatus")
class BehandlingsstatusController(
    private val skattConsumer: SkattConsumer,
    private val sjekkAvBehandlingsstatusScheduler: SjekkAvBehandlingsstatusScheduler,
) {

    @GetMapping("/behandlingsstatus")
    @Operation(
        summary = "Sjekker behandlingsstatus for en Batch-uid.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Aktive driftsavvik ble returnert.",
                content = [Content()],
            ),
        ],
    )
    fun hentBehandlingsstatus(batchUid: String): ResponseEntity<BehandlingsstatusResponse> {
        return skattConsumer.sjekkBehandlingsstatus(batchUid)
    }

    @GetMapping("/behandlingsstatusScheduled")
    @Operation(
        summary = "Manuelt starter skedulert kjøring av sjekk på behandlingsstatus.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Startet skedulert kjøring av sjekk på behandlingsstatus.",
                content = [Content()],
            ),
        ],
    )
    fun startSkedulertSjekkAvBehandlingsstatus(): ResponseEntity<Any> {
        sjekkAvBehandlingsstatusScheduler.skedulertSjekkAvBehandlingsstatus()
        return ResponseEntity.ok().build()
    }
}
