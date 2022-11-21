package no.nav.bidrag.regnskap.fil

import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.fil.overføring.FiloverføringTilElinKlient
import no.nav.bidrag.regnskap.persistence.bucket.GcpFilBucket
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.bidrag.regnskap.util.ByteArrayOutputStreamTilByteBuffer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@Component
class PåløpsfilGenerator(
  private val gcpFilBucket: GcpFilBucket,
  private val filoverføringTilElinKlient: FiloverføringTilElinKlient
) {

  companion object {
    private val LOGGER = LoggerFactory.getLogger(PåløpsfilGenerator::class.java)
    const val BATCH_BESKRIVELSE = "Kravtransaksjoner fra Bidrag-Regnskap til Predator"
    val now = LocalDate.now()
  }

  private val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

  fun skrivPåløpsfil(konteringer: List<Kontering>, påløp: Påløp) {
    val påløpsMappe = "påløp/"
    val påløpsfilnavn = "paaloop_D" + now.format(DateTimeFormatter.ofPattern("yyMMdd")).toString() + ".xml"

    if (!gcpFilBucket.finnesFil(påløpsMappe + påløpsfilnavn)) {

      val dokument = documentBuilder.newDocument()
      dokument.setXmlStandalone(true)

      val rootElement = dokument.createElementNS("http://www.trygdeetaten.no/skjema/bidrag-reskonto", "bidrag-reskonto")
      dokument.appendChild(rootElement)

      opprettStartBatchBr01(dokument, rootElement, påløp)

      var index = 0
      var sum = BigDecimal.ZERO
      finnAlleOppdragFraKonteringer(konteringer).forEach { (_, konteringerForOppdrag) ->

        if (++index % 100 == 0) {
          LOGGER.info("Påløpskjøring: Har skrevet $index av ${konteringer.size} konteringer til påløpsfil.")
        }

        val oppdragElement = dokument.createElement("oppdrag")
        rootElement.appendChild(oppdragElement)

        konteringerForOppdrag.forEach { kontering ->
          opprettKonteringBr10(dokument, oppdragElement, kontering)

          sum += kontering.oppdragsperiode!!.beløp
        }
        //opprettIdentrecordBr20(dokument, oppdragElement)
        //opprettPersionDataBr30(dokument, oppdragElement)
        //opprettKontaktInfoBr40(dokument, oppdragElement)
        //opprettAdresseInfoBr50(dokument, oppdragElement)

      }

      LOGGER.info("Påløpskjøring: Har skrevet ${konteringer.size} av ${konteringer.size} konteringer til påløpsfil.")

      opprettStoppBatchBr99(dokument, rootElement, sum, konteringer.size)

      skrivXml(dokument, påløpsMappe + påløpsfilnavn)
    }

    filoverføringTilElinKlient.lastOppFilTilFilsluse(påløpsMappe, påløpsfilnavn)

    //TODO() Overføring kontering for alle konteringer :)

    LOGGER.info("Påløpskjøring: Påløpsfil er ferdig skrevet med ${konteringer.size} konteringer og lastet opp til filsluse.")
  }

  private fun opprettStartBatchBr01(dokument: Document, rootElement: Element, påløp: Påløp) {
    val startBatchBr01 = dokument.createElement("start-batch-br01")
    rootElement.appendChild(startBatchBr01)

    val beskrivelse = dokument.createElement("beskrivelse")
    beskrivelse.textContent = BATCH_BESKRIVELSE
    startBatchBr01.appendChild(beskrivelse)

    val kjorenr = dokument.createElement("kjorenr")
    kjorenr.textContent = påløp.påløpId.toString()
    startBatchBr01.appendChild(kjorenr)

    val dato = dokument.createElement("dato")
    dato.textContent = now.toString()
    startBatchBr01.appendChild(dato)
  }

  private fun opprettKonteringBr10(dokument: Document, oppdragElement: Element, kontering: Kontering) {
    val konteringBr10Element = dokument.createElement("kontering-br10")
    oppdragElement.appendChild(konteringBr10Element)

    //Ikke i bruk, genereres tom
    val kodeFagomraade = dokument.createElement("kodeFagomraade")
    konteringBr10Element.appendChild(kodeFagomraade)

    val transKode = dokument.createElement("transKode")
    transKode.textContent = kontering.transaksjonskode
    konteringBr10Element.appendChild(transKode)

    val endring = dokument.createElement("endring")
    endring.textContent = kontering.type
    konteringBr10Element.appendChild(endring)

    val soknadType = dokument.createElement("soknadType")
    soknadType.textContent = kontering.søknadType
    konteringBr10Element.appendChild(soknadType)

    //Ikke i bruk, genereres tom
    val eierEnhet = dokument.createElement("eierEnhet")
    konteringBr10Element.appendChild(eierEnhet)

    //Ikke i bruk, genereres tom
    val behandlEnhet = dokument.createElement("behandlEnhet")
    konteringBr10Element.appendChild(behandlEnhet)

    val fagsystemId = dokument.createElement("fagsystemId")
    fagsystemId.textContent = kontering.oppdragsperiode?.sakId
    konteringBr10Element.appendChild(fagsystemId)

    val oppdragGjelderId = dokument.createElement("oppdragGjelderId")
    oppdragGjelderId.textContent = kontering.oppdragsperiode?.gjelderIdent
    konteringBr10Element.appendChild(oppdragGjelderId)

    val skyldnerId = dokument.createElement("skyldnerId")
    skyldnerId.textContent = kontering.oppdragsperiode?.oppdrag?.skyldnerIdent
    konteringBr10Element.appendChild(skyldnerId)

    val kravhaverId = dokument.createElement("kravhaverId")
    kravhaverId.textContent = kontering.oppdragsperiode?.oppdrag?.kravhaverIdent
    konteringBr10Element.appendChild(kravhaverId)

    val utbetalesTilId = dokument.createElement("utbetalesTilId")
    utbetalesTilId.textContent = kontering.oppdragsperiode?.mottakerIdent
    konteringBr10Element.appendChild(utbetalesTilId)

    val belop = dokument.createElement("belop")
    belop.textContent = kontering.oppdragsperiode?.beløp.toString() //TODO() Denne må være bigInteger
    konteringBr10Element.appendChild(belop)

    val fradragTillegg = dokument.createElement("fradragTillegg")
    fradragTillegg.textContent =
      if (Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode != null) "F" else "T" //TODO() Direkte oppgjør?
    konteringBr10Element.appendChild(fradragTillegg)

    val valutaKode = dokument.createElement("valutaKode")
    valutaKode.textContent = kontering.oppdragsperiode?.valuta
    konteringBr10Element.appendChild(valutaKode)

    val yearMonth = YearMonth.parse(kontering.overføringsperiode)

    val datoBeregnFom = dokument.createElement("datoBeregnFom")
    datoBeregnFom.textContent = LocalDate.of(yearMonth.year, yearMonth.month, 1).toString()
    konteringBr10Element.appendChild(datoBeregnFom)

    val datoBeregnTom = dokument.createElement("datoBeregnTom")
    datoBeregnTom.textContent = LocalDate.of(yearMonth.year, yearMonth.month, yearMonth.lengthOfMonth()).toString()
    konteringBr10Element.appendChild(datoBeregnTom)

    val datoVedtak = dokument.createElement("datoVedtak")
    datoVedtak.textContent = kontering.oppdragsperiode?.vedtaksdato.toString()
    konteringBr10Element.appendChild(datoVedtak)

    val datoKjores = dokument.createElement("datoKjores")
    datoKjores.textContent = LocalDate.now().toString()
    konteringBr10Element.appendChild(datoKjores)

    val saksbehId = dokument.createElement("saksbehId")
    saksbehId.textContent = kontering.oppdragsperiode?.opprettetAv
    konteringBr10Element.appendChild(saksbehId)

    val attestantId = dokument.createElement("attestantId")
    attestantId.textContent = kontering.oppdragsperiode?.opprettetAv
    konteringBr10Element.appendChild(attestantId)

    val tekst = dokument.createElement("tekst")
    tekst.textContent = kontering.oppdragsperiode?.oppdrag?.eksternReferanse
    konteringBr10Element.appendChild(tekst)

    val refFagsystemId = dokument.createElement("refFagsystemId")
    refFagsystemId.textContent = kontering.oppdragsperiode?.sakId //TODO() Hvorfor er denne to ganger
    konteringBr10Element.appendChild(refFagsystemId)

    val delytelseId = dokument.createElement("delytelseId")
    delytelseId.textContent = kontering.oppdragsperiode?.delytelseId
    konteringBr10Element.appendChild(delytelseId)

    val refDelytelseId = dokument.createElement("refDelytelseId")
    refDelytelseId.textContent = kontering.oppdragsperiode?.delytelseId //TODO() Hvorfor er denne to ganger
    konteringBr10Element.appendChild(refDelytelseId)
  }

  private fun opprettIdentrecordBr20(dokument: Document, oppdragElement: Element) {
    val identrecordBr20Element = dokument.createElement("identrecord-br20")
    oppdragElement.appendChild(identrecordBr20Element)

    //Ikke i bruk, genereres tom
    val ident = dokument.createElement("ident")
    identrecordBr20Element.appendChild(ident)

    //Ikke i bruk, genereres tom
    val kodeAltIdenttype = dokument.createElement("Kode-alt-identtype")
    identrecordBr20Element.appendChild(kodeAltIdenttype)

    //Ikke i bruk, genereres tom
    val IdAlternativ = dokument.createElement("ID-alternativ")
    identrecordBr20Element.appendChild(IdAlternativ)
  }

  private fun opprettPersionDataBr30(dokument: Document, oppdragElement: Element) {
    val personDataBr30 = dokument.createElement("person-data-br30")
    oppdragElement.appendChild(personDataBr30)

    val ident = dokument.createElement("ident") //TODO() Ny part? Skal noe inn her?
    personDataBr30.appendChild(ident)

    //Ikke i bruk, genereres tom
    val sammensattNavn = dokument.createElement("sammensattNavn")
    personDataBr30.appendChild(sammensattNavn)

    //Ikke i bruk, genereres tom
    val forNavn = dokument.createElement("forNavn")
    personDataBr30.appendChild(forNavn)

    //Ikke i bruk, genereres tom
    val mellomNavn = dokument.createElement("mellomNavn")
    personDataBr30.appendChild(mellomNavn)

    //Ikke i bruk, genereres tom
    val etterNavn = dokument.createElement("etterNavn")
    personDataBr30.appendChild(etterNavn)

    //Ikke i bruk, genereres tom
    val kodeSpraak = dokument.createElement("kodeSpraak")
    personDataBr30.appendChild(kodeSpraak)

    //Ikke i bruk, genereres tom
    val kodeSivilstand = dokument.createElement("kodeSivilstand")
    personDataBr30.appendChild(kodeSivilstand)

    //Ikke i bruk, genereres tom
    val tekstSivilstand = dokument.createElement("tekstSivilstand")
    personDataBr30.appendChild(tekstSivilstand)

    //Ikke i bruk, genereres tom
    val kodeStatsborgerskap = dokument.createElement("kodeStatsborgerskap")
    personDataBr30.appendChild(kodeStatsborgerskap)

    //Ikke i bruk, genereres tom
    val tekstStatsborgerskap = dokument.createElement("tekstStatsborgerskap")
    personDataBr30.appendChild(tekstStatsborgerskap)

    //Ikke i bruk, genereres tom
    val diskresjonskode = dokument.createElement("diskresjonskode")
    personDataBr30.appendChild(diskresjonskode)

    //Ikke i bruk, genereres tom
    val gironrInnland = dokument.createElement("gironrInnland")
    personDataBr30.appendChild(gironrInnland)

    //Ikke i bruk, genereres tom
    val gironrUtland = dokument.createElement("gironrUtland")
    personDataBr30.appendChild(gironrUtland)
  }

  private fun opprettKontaktInfoBr40(dokument: Document, oppdragElement: Element) {
    val kontaktInfoBr40 = dokument.createElement("kontakt-info-br40")
    oppdragElement.appendChild(kontaktInfoBr40)

    //Ikke i bruk, genereres tom
    val ident = dokument.createElement("ident")
    kontaktInfoBr40.appendChild(ident)

    //Ikke i bruk, genereres tom
    val kontaktperson = dokument.createElement("kontaktperson")
    kontaktInfoBr40.appendChild(kontaktperson)

    //Ikke i bruk, genereres tom
    val epost = dokument.createElement("ePost")
    kontaktInfoBr40.appendChild(epost)

    //Ikke i bruk, genereres tom
    val tlfPrivat = dokument.createElement("tlfPrivat")
    kontaktInfoBr40.appendChild(tlfPrivat)

    //Ikke i bruk, genereres tom
    val tlfMobil = dokument.createElement("tlfMobil")
    kontaktInfoBr40.appendChild(tlfMobil)

    //Ikke i bruk, genereres tom
    val tlfArbeid = dokument.createElement("tlfArbeid")
    kontaktInfoBr40.appendChild(tlfArbeid)
  }

  private fun opprettAdresseInfoBr50(dokument: Document, oppdragElement: Element) {
    val adresseInfoBr50 = dokument.createElement("adresse-info-br50")
    oppdragElement.appendChild(adresseInfoBr50)

    //Ikke i bruk, genereres tom
    val ident = dokument.createElement("ident")
    adresseInfoBr50.appendChild(ident)

    //Ikke i bruk, genereres tom
    val adressetype = dokument.createElement("adressetype")
    adresseInfoBr50.appendChild(adressetype)

    //Ikke i bruk, genereres tom
    val adresse1 = dokument.createElement("adresse1")
    adresseInfoBr50.appendChild(adresse1)

    //Ikke i bruk, genereres tom
    val adresse2 = dokument.createElement("adresse2")
    adresseInfoBr50.appendChild(adresse2)

    //Ikke i bruk, genereres tom
    val adresse3 = dokument.createElement("adresse3")
    adresseInfoBr50.appendChild(adresse3)

    //Ikke i bruk, genereres tom
    val adresse4 = dokument.createElement("adresse4")
    adresseInfoBr50.appendChild(adresse4)

    //Ikke i bruk, genereres tom
    val postnr = dokument.createElement("postnr")
    adresseInfoBr50.appendChild(postnr)

    //Ikke i bruk, genereres tom
    val poststed = dokument.createElement("poststed")
    adresseInfoBr50.appendChild(poststed)

    //Ikke i bruk, genereres tom
    val landkode = dokument.createElement("landkode")
    adresseInfoBr50.appendChild(landkode)

    //Ikke i bruk, genereres tom
    val landnavn = dokument.createElement("landnavn")
    adresseInfoBr50.appendChild(landnavn)
  }

  private fun opprettStoppBatchBr99(dokument: Document, rootElement: Element, sum: BigDecimal, antall: Int) {
    val stopBatchBr99 = dokument.createElement("stopp-batch-br99")
    rootElement.appendChild(stopBatchBr99)

    val sumBelop = dokument.createElement("sumBelop")
    sumBelop.textContent = sum.toString()
    stopBatchBr99.appendChild(sumBelop)

    val antallRecords = dokument.createElement("antallRecords")
    antallRecords.textContent = antall.toString()
    stopBatchBr99.appendChild(antallRecords)
  }

  private fun finnAlleOppdragFraKonteringer(konteringer: List<Kontering>): HashMap<Int, ArrayList<Kontering>> {
    val oppdragsMap = HashMap<Int, ArrayList<Kontering>>()

    konteringer.forEach { kontering ->
      val oppdrag = kontering.oppdragsperiode?.oppdrag?.oppdragId!!
      var current = oppdragsMap[oppdrag]
      if (current == null) {
        current = ArrayList()
        oppdragsMap[oppdrag] = current
      }
      current.add(kontering)
    }
    return oppdragsMap
  }

  private fun skrivXml(dokument: Document, påløpsfilnavn: String) {
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1")

    val source = DOMSource(dokument)
    val byteArrayStream = ByteArrayOutputStreamTilByteBuffer()
    transformer.transform(source, StreamResult(byteArrayStream))

    gcpFilBucket.lagreFil(påløpsfilnavn, byteArrayStream)

    // Output til console for testing
//    val result = StreamResult(System.out)
//    transformer.transform(source, result)
  }
}