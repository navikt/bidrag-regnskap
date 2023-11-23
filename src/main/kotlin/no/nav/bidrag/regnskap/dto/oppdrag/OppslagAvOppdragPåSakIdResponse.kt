package no.nav.bidrag.regnskap.dto.oppdrag

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "OppslagAvOppdragPåSakIdResponse",
    description = "Et oppslag på en sakId kan inneholde en eller flere oppdrag," +
        "med tilhørende oppdragsperioder, konteringer og overføring konteringer.",
)
data class OppslagAvOppdragPåSakIdResponse(

    @field:Schema(
        description = "Liste med oppdrag.",
    )
    val oppdrag: List<OppdragResponse>,
)
