package no.nav.bidrag.regnskap.påløpsgenerering

import no.nav.bidrag.regnskap.dto.Transaksjonskode
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.FileOutputStream
import java.time.LocalDate
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private val LOGGER = LoggerFactory.getLogger(PåløpsfilGenerator::class.java)

class PåløpsfilGenerator {

  private val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

  fun skrivPåløpsfil(konteringer: List<Kontering>) {
    val dokument = documentBuilder.newDocument()
    dokument.setXmlStandalone(true)

    val rootElement = dokument.createElementNS("http://www.trygdeetaten.no/skjema/bidrag-reskonto", "bidrag-reskonto")
    dokument.appendChild(rootElement)

    var index = 0
    finnAlleOppdragFraKonteringer(konteringer).forEach { (_, konteringerForOppdrag) ->

      if(index % 100 == 0) {
        LOGGER.info("Påløpskjøring: Har skrevet $index av ${konteringer.size} konteringer til påløpsfil.")
      }

      val oppdragElement = dokument.createElement("oppdrag")
      rootElement.appendChild(oppdragElement)

      konteringerForOppdrag.forEach { kontering ->
        opprettKonteringBr10(dokument, oppdragElement, kontering)
      }
      //opprettIdentrecordBr20(dokument, oppdragElement)
      //opprettPersionDataBr30(dokument, oppdragElement)
      //opprettKontaktInfoBr40(dokument, oppdragElement)
      //opprettAdresseInfoBr50(dokument, oppdragElement)

      index++
    }
    skrivXml(dokument)
    LOGGER.info("Påløpskjøring: Påløpsfil er ferdig skrevet med ${konteringer.size} konteringer og lastet opp til filsluse.")
  }

  private fun opprettKonteringBr10(dokument: Document, oppdragElement: Element, kontering: Kontering) {
    val konteringBr10Element = dokument.createElement("kontering-br10")
    oppdragElement.appendChild(konteringBr10Element)

    //Ikke i bruk, genereres tom
    //val kodeFagomraade = dokument.createElement("kodeFagomraade")
    //konteringBr10Element.appendChild(kodeFagomraade)

    val transKode = dokument.createElement("transKode")
    transKode.textContent = kontering.transaksjonskode
    konteringBr10Element.appendChild(transKode)

    val endring = dokument.createElement("endring")
    endring.textContent = kontering.type
    konteringBr10Element.appendChild(endring)

    val soknadType = dokument.createElement("soknadType")
    soknadType.textContent = kontering.justering
    konteringBr10Element.appendChild(soknadType)

    //Ikke i bruk, genereres tom
    //val eierEnhet = dokument.createElement("eierEnhet")
    //konteringBr10Element.appendChild(eierEnhet)

    //Ikke i bruk, genereres tom
    //val behandlEnhet = dokument.createElement("behandlEnhet")
    //konteringBr10Element.appendChild(behandlEnhet)

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
      if (Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode != null) "F" else "T" //TODO() Aner ikke om dette stemmer.. Direkte oppgjør?
    konteringBr10Element.appendChild(fradragTillegg)

    val datoBeregnFom = dokument.createElement("datoBeregnFom")
    datoBeregnFom.textContent =
      kontering.overføringsperiode + "-01"
    konteringBr10Element.appendChild(datoBeregnFom)

    val datoBeregnTom = dokument.createElement("datoBeregnTom")
    datoBeregnTom.textContent =
      kontering.overføringsperiode + "-01" //TODO() Denne skal være siste dag i mnd hurra
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
    val ident = dokument.createElement("Ident")
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
    val ident = dokument.createElement("Ident")
    kontaktInfoBr40.appendChild(ident)

    //Ikke i bruk, genereres tom
    val kontaktperson = dokument.createElement("Kontaktperson")
    kontaktInfoBr40.appendChild(kontaktperson)

    //Ikke i bruk, genereres tom
    val epost = dokument.createElement("e-post")
    kontaktInfoBr40.appendChild(epost)

    //Ikke i bruk, genereres tom
    val tlfPrivat = dokument.createElement("Tlf-privat")
    kontaktInfoBr40.appendChild(tlfPrivat)

    //Ikke i bruk, genereres tom
    val tlfMobil = dokument.createElement("Tlf-mobil")
    kontaktInfoBr40.appendChild(tlfMobil)

    //Ikke i bruk, genereres tom
    val tlfArbeid = dokument.createElement("Tlf-arbeid")
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

  private fun skrivXml(dokument: Document) {
    val transformer = TransformerFactory.newInstance().newTransformer()

    //Pretty print
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");

    val source = DOMSource(dokument)
    val out = System.out
//    val out = FileOutputStream("C:\\Users\\H165990\\Documents\\Dev\\bidrag-regnskap\\påløpsfil.xml")
    //TODO() Filsluse. Hva, hvor, hvordan
    val result = StreamResult(out)

    transformer.transform(source, result)
  }
}