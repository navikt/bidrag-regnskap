package no.nav.bidrag.regnskap.dto.behandlingsstatus

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode

@Schema(name = "Behandlingstatus response", description = "Response fra kall mot behandlingsstatus med batch-uid.")
data class BehandlingsstatusResponse(
    val konteringFeil: List<Feilmelding>,
    val batchStatus: Batchstatus,
    val batchUid: String,
    val totaltAntall: Int,
    val mislyketAntall: Int,
    val fullfoertAntall: Int
)

@Schema(name = "Behandlingstatus feilmelding", description = "Feilmelding i responsen ved kall mot behandlingsstatus med batch-uid.")
data class Feilmelding(
    val feilkode: String?,
    val fagsystemId: String?,
    val transaksjonskode: Transaksjonskode?,
    val delytelseId: String?,
    val periode: String?,
    val feilmelding: String
)

enum class Batchstatus {
    Failed,
    Processing,
    Done,
    DoneWithWarnings
}
