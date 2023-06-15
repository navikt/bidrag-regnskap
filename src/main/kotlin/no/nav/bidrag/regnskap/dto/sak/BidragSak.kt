package no.nav.bidrag.regnskap.dto.sak

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.regnskap.dto.sak.enumer.Bidragssakstatus
import no.nav.bidrag.regnskap.dto.sak.enumer.Sakskategori

@Schema(description = "Metadata for en bidragssak")
data class BidragSak(
    @Schema(description = "Eierfogd for bidragssaken")
    val eierfogd: String,
    @Schema(description = "Saksnummeret til bidragssaken")
    val saksnummer: String,
    @Schema(description = "Saksstatus til bidragssaken")
    val saksstatus: Bidragssakstatus,
    @Schema(description = "Kategorikode: 'N' eller 'U'")
    val kategori: Sakskategori,
    @Schema(description = "Om saken omhandler paragraf 19")
    val erParagraf19: Boolean = false,
    @Schema(description = "Om saken inneholder personer med diskresjonskode")
    val begrensetTilgang: Boolean = false,
    @Schema(description = "Rollene som saken inneholder")
    val roller: List<RolleDto> = emptyList()
)
