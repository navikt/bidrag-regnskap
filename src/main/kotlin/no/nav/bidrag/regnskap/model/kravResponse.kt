package no.nav.bidrag.regnskap.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.YearMonthSerializer
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import java.time.YearMonth

@Schema(name = "Kravfeil", description = "Lister feil i et krav.")
data class KravResponse(
  val konteringsfeil: List<Konteringsfeil>
)

@Schema(name = "Konteringsfeil", description = "Beskriver feil i en enkelt kontering.")
data class Konteringsfeil(

  @field:Schema(
    description = "En kode som angir type feil som har oppstått. " +
        "Feilkoden er ment å kunne brukes til å maskinelt sortere feil.",
    example = "TOLKNING")
  val feilkode: String,

  @field:Schema(
    description = "En beskrivelse av feilen som har oppstått. " +
        "Feilmeldingen er ment å være forståelig for et menneske ved manuell gjennomgang.",
    example = "Tolkning feilet i Elin.")
  val feilmelding: String,

  @field:Schema(description = "Identifiserer hvilken kontering som førte til feilen.")
  val konteringId: KonteringId
)

@Schema(name = "KonteringId", description = "Identifiserer en kontering unikt.")
data class KonteringId(

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

  @field:Schema(description = "Unik referanse til perioden i vedtaket. " +
      "I bidragssaken kan en periode strekke over flere måneder, og samme referanse blir da benyttet for alle månedene. " +
      "Samme referanse kan ikke benyttes to ganger for samme transaksjonskode i samme måned.",
    example = "123456789",
    required = true)
  val delytelsesId: String
)
