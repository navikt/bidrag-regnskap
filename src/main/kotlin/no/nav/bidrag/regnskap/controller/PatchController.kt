package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.regnskap.service.OppdragService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PatchController(private val oppdragService: OppdragService) {

    @Protected
    @PostMapping("/patchMottaker")
    fun sendMelding(@RequestBody patchMottakerRequest: PatchMottakerRequest) {
        oppdragService.patchMottaker(patchMottakerRequest.saksnummer, patchMottakerRequest.kravhaver, patchMottakerRequest.mottaker)
    }
}

data class PatchMottakerRequest(

    @field:Schema(
        description = "Saksnummer som skal korrigeres.",
        example = "123456",
    )
    val saksnummer: Saksnummer,

    @field:Schema(
        description = "Ident til kravhaver.",
        example = "00000000000",
    )
    val kravhaver: Personident,

    @field:Schema(
        description = "Ident til ny mottaker.",
        example = "00000000000",
    )
    val mottaker: Personident,
)
