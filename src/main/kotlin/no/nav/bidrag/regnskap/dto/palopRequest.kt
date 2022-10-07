package no.nav.bidrag.regnskap.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate
import java.time.YearMonth

@Schema(description = "Et påløp representerer en kjøring av overføring av påløpsfil til skatt.")
data class PalopRequest(

  @field:Schema(
    description = "Dato paløpet skal kjøre.",
    example = "2022-01-01",
    required = true
  )
  @field:DateTimeFormat(pattern = "yyyy-MM-dd")
  val kjoredato: LocalDate,

  @field:Schema(
    description = "Perioden påløpet gjelder for.",
    type = "String",
    format = "yyyy-MM",
    example = "2022-01",
    required = true
  )
  val forPeriode: YearMonth
)
