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

@Component
class AvstemningsfilGenerator(
  private val gcpFilBucket: GcpFilBucket,
  private val filoverføringTilElinKlient: FiloverføringTilElinKlient
) {

  companion object {
    private val LOGGER = LoggerFactory.getLogger(AvstemningsfilGenerator::class.java)
  }

  fun skrivAvstemningfil(konteringer: List<Kontering>, now: LocalDate) {
    LOGGER.info("Starter bygging av avstemningKontering- og avstemningSummeringsfil for $now.")

    val nowFormattert = now.format(DateTimeFormatter.ofPattern("yyMMdd")).toString()
    val avstemningMappe = "avstemning/"
    val avstemningKonteringFilnavn = "avstdet_D$nowFormattert.xml"
    val avstemningSummeringFilnavn = "avstsum_D$nowFormattert.xml"

    if (!gcpFilBucket.finnesFil(avstemningMappe + avstemningKonteringFilnavn)) {
      val summeringer = opprettAvstemningsfilSummeringer()

      val avstemningsfilBuffer = opprettAvstemningFil(konteringer, summeringer, nowFormattert)
      gcpFilBucket.lagreFil(avstemningMappe + avstemningKonteringFilnavn, avstemningsfilBuffer)

      val avstemningSummeringFil = opprettAvstemningSummeringFil(summeringer)
      gcpFilBucket.lagreFil(avstemningMappe + avstemningSummeringFilnavn, avstemningSummeringFil)
    }

    filoverføringTilElinKlient.lastOppFilTilFilsluse(avstemningMappe, avstemningKonteringFilnavn)
    filoverføringTilElinKlient.lastOppFilTilFilsluse(avstemningMappe, avstemningSummeringFilnavn)

    LOGGER.info("AvstemningKontering- og avstemningSummeringsfil er ferdig skrevet med ${konteringer.size} konteringer og lastet opp til filsluse.")
  }

  private fun opprettAvstemningFil(
    konteringer: List<Kontering>,
    summering: Map<String, AvstemningsfilSummeringer>,
    now: String
  ): ByteArrayOutputStreamTilByteBuffer {
    val avstemningsfilBuffer = ByteArrayOutputStreamTilByteBuffer()

    konteringer.forEach { kontering ->
      val transaksjonskodeSummering = summering[kontering.transaksjonskode]!!
      val periode = YearMonth.parse(kontering.overføringsperiode)

      avstemningsfilBuffer.write(
        (kontering.transaksjonskode + ";"
            + kontering.oppdragsperiode!!.sakId + ";"
            + kontering.oppdragsperiode.beløp.toString() + ";"
            + LocalDate.of(periode.year, periode.month, 1).format(DateTimeFormatter.ofPattern("yyMMdd")).toString() + ";"
            + LocalDate.of(periode.year, periode.month, periode.lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyMMdd")).toString() + ";"
            + now + ";"
            + "F" + ";" //TODO
            + kontering.oppdragsperiode.delytelseId + ";"
            + kontering.oppdragsperiode.mottakerIdent + ";"
            + kontering.oppdragsperiode.oppdrag!!.kravhaverIdent + ";"
            + "\n")
          .toByteArray()
      )

      transaksjonskodeSummering.sum += kontering.oppdragsperiode.beløp
      transaksjonskodeSummering.antallKonteringer++
    }
    return avstemningsfilBuffer
  }

  private fun opprettAvstemningSummeringFil(summering: Map<String, AvstemningsfilSummeringer>): ByteArrayOutputStreamTilByteBuffer {
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
        totalSum += avstemningSummering.sum
        totalAntall++
      }
    }

    avstemningSummeringFil.write(
      ("Total:;"
          + totalSum + ";"
          + "T;" //TODO() Avklare hva dette er for noe??
          + totalAntall + ";")
        .toByteArray()
    )
    return avstemningSummeringFil
  }

  private fun opprettAvstemningsfilSummeringer(): Map<String, AvstemningsfilSummeringer> {
    val summering = mutableMapOf<String, AvstemningsfilSummeringer>()
    Transaksjonskode.values().forEach {
      summering[it.name] = AvstemningsfilSummeringer(it, BigDecimal(0), 0)
    }
    return summering
  }
}