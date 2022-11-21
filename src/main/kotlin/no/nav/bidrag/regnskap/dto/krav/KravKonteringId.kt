package no.nav.bidrag.regnskap.dto.krav

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.YearMonthSerializer
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import org.springframework.format.annotation.DateTimeFormat
import java.time.YearMonth

@Schema(name = "KonteringId", description = "Identifiserer en kontering unikt.")
data class KravKonteringId(

  @field:Schema(description = "Type transaksjon.", example = "B1", required = true)
  val transaksjonskode: Transaksjonskode,

  @field:Schema(
    description = "Angir hvilken periode (måned og år) konteringen gjelder.",
    type = "String",
    format = "yyyy-mm",
    example = "2022-04",
    required = true
  )
  @field:DateTimeFormat(pattern = "yyyy-MM")
  @field:JsonSerialize(using = YearMonthSerializer::class)
  @field:JsonDeserialize(using = YearMonthDeserializer::class)
  val periode: YearMonth,

  @field:Schema(
    description = "Unik referanse til oppdragsperioden i vedtaket. " +
        "I bidragssaken kan en oppdragsperiode strekke over flere måneder, og samme referanse blir da benyttet for alle månedene. " +
        "Samme referanse kan ikke benyttes to ganger for samme transaksjonskode i samme måned.",
    example = "123456789",
    required = true
  )
  val delytelsesId: String
)