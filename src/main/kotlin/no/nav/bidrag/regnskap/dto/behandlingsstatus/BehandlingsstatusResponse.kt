package no.nav.bidrag.regnskap.dto.behandlingsstatus

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Behandlingstatus response", description = "Response fra kall mot behandlingsstatus med batch-uid.")
data class BehandlingsstatusResponse(
    val konteringFeil: List<Feilmelding>,
    val batchStatus: Batchstatus,
    val totaltAntall: Int,
    val mislyketAntall: Int,
    val fullfoertAntall: Int
)

@Schema(name = "Behandlingstatus feilmelding", description = "Feilmelding i responsen ved kall mot behandlingsstatus med batch-uid.")
data class Feilmelding(

    val feilmelding: String
)

enum class Batchstatus {
    Failed,
    Done
}
