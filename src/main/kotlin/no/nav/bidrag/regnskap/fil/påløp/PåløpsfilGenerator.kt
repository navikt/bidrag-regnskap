package no.nav.bidrag.regnskap.fil.påløp

import kotlinx.coroutines.yield
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.dto.enumer.Type
import no.nav.bidrag.regnskap.fil.overføring.FiloverføringTilElinKlient
import no.nav.bidrag.regnskap.persistence.bucket.GcpFilBucket
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.bidrag.regnskap.service.PåløpskjøringLytter
import no.nav.bidrag.regnskap.util.ByteArrayOutputStreamTilByteBuffer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.function.Consumer
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
    }

    private var lyttere: List<PåløpskjøringLytter> = emptyList()
    private val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    suspend fun skrivPåløpsfilOgLastOppPåFilsluse(
        konteringer: List<Kontering>,
        påløp: Påløp,
        lyttere: List<PåløpskjøringLytter>
    ) {
        val now = LocalDate.now()
        val påløpsMappe = "påløp/"
        val påløpsfilnavn = "paaloop_D" + now.format(DateTimeFormatter.ofPattern("yyMMdd")).toString() + ".xml"
        this.lyttere = lyttere

        val dokument = documentBuilder.newDocument()
        dokument.xmlStandalone = true

        val rootElement =
            dokument.createElementNS("http://www.trygdeetaten.no/skjema/bidrag-reskonto", "bidrag-reskonto")
        dokument.appendChild(rootElement)

        opprettStartBatchBr01(dokument, rootElement, påløp, now)

        var index = 0
        var sum = BigDecimal.ZERO
        finnAlleOppdragFraKonteringer(konteringer).forEach { (_, konteringerForOppdrag) ->
            yield()

            if (++index % 100 == 0) {
                medLyttere {it.rapportertKonteringerSkrevetTilFil(påløp, index, konteringer.size)}
            }

            val oppdragElement = dokument.createElement("oppdrag")
            rootElement.appendChild(oppdragElement)

            konteringerForOppdrag.forEach { kontering ->
                yield()
                opprettKonteringBr10(dokument, oppdragElement, kontering, now)

                sum += kontering.oppdragsperiode!!.beløp
            }
        }

        LOGGER.info("Påløpskjøring: Har skrevet ${konteringer.size} av ${konteringer.size} konteringer til påløpsfil.")

        opprettStoppBatchBr99(dokument, rootElement, sum, konteringer.size)
        skrivXml(dokument, påløpsMappe + påløpsfilnavn)

        filoverføringTilElinKlient.lastOppFilTilFilsluse(påløpsMappe, påløpsfilnavn)
    }

    private fun opprettStartBatchBr01(dokument: Document, rootElement: Element, påløp: Påløp, now: LocalDate) {
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

    private fun opprettKonteringBr10(
        dokument: Document,
        oppdragElement: Element,
        kontering: Kontering,
        now: LocalDate
    ) {
        val konteringBr10Element = dokument.createElement("kontering-br10")
        oppdragElement.appendChild(konteringBr10Element)

        // Ikke i bruk, genereres tom
        val kodeFagomraade = dokument.createElement("kodeFagomraade")
        konteringBr10Element.appendChild(kodeFagomraade)

        val transKode = dokument.createElement("transKode")
        transKode.textContent = kontering.transaksjonskode
        konteringBr10Element.appendChild(transKode)

        val endring = dokument.createElement("endring")
        endring.textContent = if (kontering.type == Type.NY.name) "N" else "J"
        konteringBr10Element.appendChild(endring)

        val soknadType = dokument.createElement("soknadType")
        soknadType.textContent = kontering.søknadType
        konteringBr10Element.appendChild(soknadType)

        // Ikke i bruk, genereres tom
        val eierEnhet = dokument.createElement("eierEnhet")
        konteringBr10Element.appendChild(eierEnhet)

        // Ikke i bruk, genereres tom
        val behandlEnhet = dokument.createElement("behandlEnhet")
        konteringBr10Element.appendChild(behandlEnhet)

        val fagsystemId = dokument.createElement("fagsystemId")
        fagsystemId.textContent = kontering.oppdragsperiode?.oppdrag?.sakId
        konteringBr10Element.appendChild(fagsystemId)

        val oppdragGjelderId = dokument.createElement("oppdragGjelderId")
        oppdragGjelderId.textContent = kontering.oppdragsperiode?.oppdrag?.gjelderIdent
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
        belop.textContent = kontering.oppdragsperiode?.beløp.toString()
        konteringBr10Element.appendChild(belop)

        val fradragTillegg = dokument.createElement("fradragTillegg")
        fradragTillegg.textContent =
            if (Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode != null) "T" else "F"
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
        datoKjores.textContent = now.toString()
        konteringBr10Element.appendChild(datoKjores)

        val saksbehId = dokument.createElement("saksbehId")
        saksbehId.textContent = kontering.oppdragsperiode?.opprettetAv
        konteringBr10Element.appendChild(saksbehId)

        val attestantId = dokument.createElement("attestantId")
        attestantId.textContent = kontering.oppdragsperiode?.opprettetAv
        konteringBr10Element.appendChild(attestantId)

        val tekst = dokument.createElement("tekst")
        tekst.textContent = kontering.oppdragsperiode?.eksternReferanse
        konteringBr10Element.appendChild(tekst)

        val refFagsystemId = dokument.createElement("refFagsystemId")
        refFagsystemId.textContent = kontering.oppdragsperiode?.oppdrag?.sakId
        konteringBr10Element.appendChild(refFagsystemId)

        val delytelseId = dokument.createElement("delytelseId")
        delytelseId.textContent = kontering.oppdragsperiode?.delytelseId.toString()
        konteringBr10Element.appendChild(delytelseId)

        val refDelytelseId = dokument.createElement("refDelytelseId")
        refDelytelseId.textContent = kontering.oppdragsperiode?.delytelseId.toString()
        konteringBr10Element.appendChild(refDelytelseId)
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

    private inline fun medLyttere(lytterConsumer: Consumer<PåløpskjøringLytter>) = lyttere.forEach(lytterConsumer)

}
