package no.nav.bidrag.regnskap.dto.krav

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "KravResponse", description = "Svar med referanse til videre behandling av konteringene.")
data class KravResponse(
  val batchUid: String
)

