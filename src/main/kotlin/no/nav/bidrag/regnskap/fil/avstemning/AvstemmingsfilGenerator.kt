package no.nav.bidrag.regnskap.fil.avstemning

import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.fil.overføring.FiloverføringTilElinKlient
import no.nav.bidrag.regnskap.persistence.bucket.GcpFilBucket
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.util.ByteArrayOutputStreamTilByteBuffer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val LOGGER = LoggerFactory.getLogger(AvstemmingsfilGenerator::class.java)

@Component
class AvstemmingsfilGenerator(
  private val gcpFilBucket: GcpFilBucket,
  private val filoverføringTilElinKlient: FiloverføringTilElinKlient
) {

  fun skrivAvstemmingsfil(konteringer: List<Kontering>, now: LocalDate) {
    LOGGER.info("Starter bygging av avstemningKontering- og avstemningSummeringsfil for $now.")

    val nowFormattert = now.format(DateTimeFormatter.ofPattern("yyMMdd")).toString()
    val avstemmingMappe = "avstemning/"
    val avstemmingKonteringFilnavn = "avstdet_D$nowFormattert.xml"
    val avstemmingSummeringFilnavn = "avstsum_D$nowFormattert.xml"

    if (!gcpFilBucket.finnesFil(avstemmingMappe + avstemmingKonteringFilnavn)) {
      val summeringer = opprettAvstemmingsfilSummeringer()

      val avstemningsfilBuffer = opprettAvstemmingFil(konteringer, summeringer, nowFormattert)
      gcpFilBucket.lagreFil(avstemmingMappe + avstemmingKonteringFilnavn, avstemningsfilBuffer)

      val avstemningSummeringFil = opprettAvstemmingSummeringFil(summeringer)
      gcpFilBucket.lagreFil(avstemmingMappe + avstemmingSummeringFilnavn, avstemningSummeringFil)
    }

    filoverføringTilElinKlient.lastOppFilTilFilsluse(avstemmingMappe, avstemmingKonteringFilnavn)
    filoverføringTilElinKlient.lastOppFilTilFilsluse(avstemmingMappe, avstemmingSummeringFilnavn)

    LOGGER.info("AvstemmingKontering- og avstemmingSummeringsfil er ferdig skrevet med ${konteringer.size} konteringer og lastet opp til filsluse.")
  }

  private fun opprettAvstemmingFil(
    konteringer: List<Kontering>,
    summering: Map<String, AvstemmingsfilSummeringer>,
    now: String
  ): ByteArrayOutputStreamTilByteBuffer {
    val avstemningsfilBuffer = ByteArrayOutputStreamTilByteBuffer()

    konteringer.forEachIndexed { index, kontering ->
      val transaksjonskodeSummering = summering[kontering.transaksjonskode]!!
      val periode = YearMonth.parse(kontering.overføringsperiode)

      avstemningsfilBuffer.write(
        (kontering.transaksjonskode + ";"
            + kontering.oppdragsperiode!!.oppdrag!!.sakId + ";"
            + kontering.oppdragsperiode.beløp.toString() + ";"
            + LocalDate.of(periode.year, periode.month, 1).format(DateTimeFormatter.ofPattern("yyMMdd")).toString() + ";"
            + LocalDate.of(periode.year, periode.month, periode.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyMMdd")).toString() + ";"
            + now + ";"
            + if (Transaksjonskode.valueOf(kontering.transaksjonskode).negativtBeløp) "F" else "T"
            + kontering.oppdragsperiode.delytelseId.toString() + ";"
            + kontering.oppdragsperiode.mottakerIdent + ";"
            + kontering.oppdragsperiode.oppdrag!!.kravhaverIdent + ";"
            + "\n")
          .toByteArray()
      )

      if (index+1 % 100 == 0) {
        LOGGER.info("Påløpskjøring: Har skrevet $index av ${konteringer.size} konteringer til avstemningsfil...")
      }

      transaksjonskodeSummering.sum += kontering.oppdragsperiode.beløp
      transaksjonskodeSummering.antallKonteringer++
    }
    return avstemningsfilBuffer
  }

  private fun opprettAvstemmingSummeringFil(summering: Map<String, AvstemmingsfilSummeringer>): ByteArrayOutputStreamTilByteBuffer {
    val avstemningSummeringFil = ByteArrayOutputStreamTilByteBuffer()

    var totalSum = BigDecimal.ZERO
    var totalAntall = 0

    summering.forEach { name, avstemningSummering ->
      if (avstemningSummering.antallKonteringer != 0) {
        avstemningSummeringFil.write(
          (name + ";"
              + avstemningSummering.sum + ";"
              + avstemningSummering.fradragEllerTillegg + ";"
              + avstemningSummering.antallKonteringer + ";"
              + "\n")
            .toByteArray()
        )
        totalSum += if(avstemningSummering.transaksjonskode.negativtBeløp) avstemningSummering.sum.negate() else avstemningSummering.sum
        totalAntall++
      }
    }

    avstemningSummeringFil.write(
      ("Total:;"
          + totalSum + ";"
          + if(totalSum >= BigDecimal.ZERO) "T;" else "F;"
          + totalAntall + ";")
        .toByteArray()
    )
    return avstemningSummeringFil
  }

  private fun opprettAvstemmingsfilSummeringer(): Map<String, AvstemmingsfilSummeringer> {
    val summering = mutableMapOf<String, AvstemmingsfilSummeringer>()
    Transaksjonskode.values().forEach {
      summering[it.name] = AvstemmingsfilSummeringer(it, BigDecimal(0), 0)
    }
    return summering
  }
}