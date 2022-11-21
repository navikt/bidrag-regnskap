package no.nav.bidrag.regnskap.dto.enumer

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description = "Konteringstypen er NY for nye konteringer for en stønad i en periode. " +
      "Deretter skal alle konteringer for samme stønad i samme periode markere ENDRING, altså B3-konteringen og for alle påfølgende B1-konteringer."
)
enum class Type {
  NY, ENDRING
}
