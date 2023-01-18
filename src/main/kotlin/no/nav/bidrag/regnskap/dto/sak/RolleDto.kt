package no.nav.bidrag.regnskap.dto.sak

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.regnskap.dto.sak.enumer.Rolletype

@JsonIgnoreProperties(ignoreUnknown = true)
data class RolleDto(
  @JsonProperty("fodselsnummer")
  val fødselsnummer: String? = null,
  @Schema(description = "Kode for rolletype tilsvarende kodene i T_KODE_ROLLETYPE.")
  val type: Rolletype,
  val objektnummer: String? = null,
  val reellMottager: String? = null,
  val mottagerErVerge: Boolean = false,
  val samhandlerIdent: String? = null
)