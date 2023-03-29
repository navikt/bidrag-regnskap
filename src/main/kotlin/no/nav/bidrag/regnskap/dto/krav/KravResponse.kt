package no.nav.bidrag.regnskap.dto.krav

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "KravResponse", description = "Svar med referanse til videre behandling av konteringene.")
data class KravResponse(

  @JsonProperty("BatchUid")
  val batchUid: String
)

