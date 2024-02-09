package no.nav.bidrag.regnskap.controller

import no.nav.bidrag.regnskap.dto.patch.PatchMottakerRequest
import no.nav.bidrag.regnskap.service.OppdragService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
class PatchController(private val oppdragService: OppdragService) {

    @PostMapping("/patchMottaker")
    fun patchMottaker(@RequestBody patchMottakerRequest: PatchMottakerRequest) {
        oppdragService.patchMottaker(patchMottakerRequest.saksnummer, patchMottakerRequest.kravhaver, patchMottakerRequest.mottaker)
    }
}
