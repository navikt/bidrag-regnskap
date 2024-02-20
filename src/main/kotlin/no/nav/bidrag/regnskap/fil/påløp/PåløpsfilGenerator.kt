package no.nav.bidrag.regnskap.fil.påløp

import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.domene.enums.regnskap.Type
import no.nav.bidrag.regnskap.fil.overføring.FiloverføringTilElinKlient
import no.nav.bidrag.regnskap.persistence.bucket.GcpFilBucket
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.bidrag.regnskap.service.PersistenceService
import no.nav.bidrag.regnskap.service.PåløpskjøringLytter
import no.nav.bidrag.regnskap.util.ByteArrayOutputStreamTilByteBuffer
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.function.Consumer
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

class PåløpsfilGenerator(
    private val gcpFilBucket: GcpFilBucket,
    private val filoverføringTilElinKlient: FiloverføringTilElinKlient,
    private val persistenceService: PersistenceService,
) {

    private var lyttere: List<PåløpskjøringLytter> = emptyList()

    fun skrivPåløpsfilOgLastOppPåFilsluse(påløp: Påløp, lyttere: List<PåløpskjøringLytter>) {
        this.lyttere = lyttere
        val now = LocalDate.now()
        val konteringer = persistenceService.hentAlleKonteringerForPeriodeOgSomIkkeErOverførtEnda(påløp.forPeriode)

        val byteArrayOutputStream = ByteArrayOutputStreamTilByteBuffer()
        val writer = XMLOutputFactory.newInstance().createXMLStreamWriter(byteArrayOutputStream, "ISO-8859-1")

        skrivHeader(writer)
        skrivStartBatchBr01(writer, påløp, now)

        var index = 0
        var sum = BigDecimal.ZERO
        finnAlleSakerFraKonteringer(konteringer, påløp).forEach { (_, konteringerForSak) ->
            if (++index % 10000 == 0) {
                medLyttere { it.rapportertKonteringerSkrevetTilFil(påløp, index, konteringer.size) }
            }

            writer.writeCharacters("\n")
            writer.writeStartElement("oppdrag")

            konteringerForSak.forEach { kontering ->
                skrivKonteringBr10(writer, kontering, now)
                if (Transaksjonskode.valueOf(kontering.transaksjonskode).negativtBeløp) {
                    sum -= kontering.oppdragsperiode!!.beløp
                } else {
                    sum += kontering.oppdragsperiode!!.beløp
                }
            }

            writer.writeEndElement()
        }

        medLyttere { it.konteringerSkrevetTilFilFerdig(påløp, konteringer.size) }

        skrivStoppBatchBr99(writer, sum, konteringer.size)

        skrivHeaderSlutt(writer)

        writer.close()
        byteArrayOutputStream.close()

        val påløpsMappe = "påløp/"
        val påløpsfilnavn = "paaloop_D" + now.format(DateTimeFormatter.ofPattern("yyMMdd")).toString() + ".xml"

        medLyttere { it.lastOppFilTilGcpBucket(påløp, "Starter opplasting til GCP bucket..") }
        gcpFilBucket.lagreFil(påløpsMappe + påløpsfilnavn, byteArrayOutputStream)
        medLyttere { it.lastOppFilTilGcpBucket(påløp, "Fil lastet opp til GCP bucket!") }

        medLyttere { it.lastOppFilTilFilsluse(påløp, "Starter opplasting til filsluse..") }
        filoverføringTilElinKlient.lastOppFilTilFilsluse(påløpsMappe, påløpsfilnavn)
        medLyttere { it.lastOppFilTilFilsluse(påløp, "Fil lastet opp til filsluse!") }

// For lokal testing av filgenerering
//        val xmlString = byteArrayOutputStream.toString()
//        println("Generert XML:\n$xmlString")
    }

    private fun skrivHeader(writer: XMLStreamWriter) {
        writer.writeStartDocument("ISO-8859-1", "1.0")
        writer.writeCharacters("\n")
        writer.writeStartElement("bidrag-reskonto")
        writer.writeDefaultNamespace("http://www.trygdeetaten.no/skjema/bidrag-reskonto")
    }

    private fun skrivHeaderSlutt(writer: XMLStreamWriter) {
        writer.writeCharacters("\n")
        writer.writeEndElement() // bidrag-reskonto
        writer.writeEndDocument()
    }

    private fun skrivStartBatchBr01(writer: XMLStreamWriter, påløp: Påløp, now: LocalDate) {
        writer.writeCharacters("\n")
        writer.writeStartElement("start-batch-br01")

        writer.writeStartElement("beskrivelse")
        writer.writeCharacters("Kravtransaksjoner fra Bidrag-Regnskap til Predator")
        writer.writeEndElement()

        writer.writeStartElement("kjorenr")
        writer.writeCharacters(påløp.påløpId.toString())
        writer.writeEndElement()

        writer.writeStartElement("dato")
        writer.writeCharacters(now.toString())
        writer.writeEndElement()
        writer.writeEndElement() // start-batch-br01
    }

    private fun skrivKonteringBr10(writer: XMLStreamWriter, kontering: Kontering, now: LocalDate) {
        writer.writeStartElement("kontering-br10")

        // Ikke i bruk, genereres tom
        writer.writeStartElement("kodeFagomraade")
        writer.writeEndElement()

        writer.writeStartElement("transKode")
        writer.writeCharacters(kontering.transaksjonskode)
        writer.writeEndElement()

        writer.writeStartElement("endring")
        writer.writeCharacters(if (kontering.type == Type.NY.name) "N" else "J")
        writer.writeEndElement()

        writer.writeStartElement("soknadType")
        writer.writeCharacters(kontering.søknadType)
        writer.writeEndElement()

        // Ikke i bruk, genereres tom
        writer.writeStartElement("eierEnhet")
        writer.writeEndElement()

        // Ikke i bruk, genereres tom
        writer.writeStartElement("behandlEnhet")
        writer.writeEndElement()

        writer.writeStartElement("fagsystemId")
        writer.writeCharacters(kontering.oppdragsperiode?.oppdrag?.sakId)
        writer.writeEndElement()

        writer.writeStartElement("oppdragGjelderId")
        writer.writeCharacters(kontering.oppdragsperiode?.oppdrag?.gjelderIdent)
        writer.writeEndElement()

        writer.writeStartElement("skyldnerId")
        writer.writeCharacters(kontering.oppdragsperiode?.oppdrag?.skyldnerIdent)
        writer.writeEndElement()

        writer.writeStartElement("kravhaverId")
        writer.writeCharacters(kontering.oppdragsperiode?.oppdrag?.kravhaverIdent)
        writer.writeEndElement()

        writer.writeStartElement("utbetalesTilId")
        writer.writeCharacters(kontering.oppdragsperiode?.oppdrag?.mottakerIdent)
        writer.writeEndElement()

        writer.writeStartElement("belop")
        writer.writeCharacters(kontering.oppdragsperiode?.beløp.toString())
        writer.writeEndElement()

        writer.writeStartElement("fradragTillegg")
        writer.writeCharacters(if (Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode != null) "T" else "F")
        writer.writeEndElement()

        writer.writeStartElement("valutaKode")
        writer.writeCharacters(kontering.oppdragsperiode?.valuta)
        writer.writeEndElement()

        val yearMonth = YearMonth.parse(kontering.overføringsperiode)

        writer.writeStartElement("datoBeregnFom")
        writer.writeCharacters(LocalDate.of(yearMonth.year, yearMonth.month, 1).toString())
        writer.writeEndElement()

        writer.writeStartElement("datoBeregnTom")
        writer.writeCharacters(LocalDate.of(yearMonth.year, yearMonth.month, yearMonth.lengthOfMonth()).toString())
        writer.writeEndElement()

        writer.writeStartElement("datoVedtak")
        writer.writeCharacters(kontering.oppdragsperiode?.vedtaksdato.toString())
        writer.writeEndElement()

        writer.writeStartElement("datoKjores")
        writer.writeCharacters(now.toString())
        writer.writeEndElement()

        writer.writeStartElement("saksbehId")
        writer.writeCharacters(kontering.oppdragsperiode?.opprettetAv)
        writer.writeEndElement()

        writer.writeStartElement("attestantId")
        writer.writeCharacters(kontering.oppdragsperiode?.opprettetAv)
        writer.writeEndElement()

        writer.writeStartElement("tekst")
        writer.writeCharacters(kontering.oppdragsperiode?.eksternReferanse)
        writer.writeEndElement()

        writer.writeStartElement("refFagsystemId")
        writer.writeCharacters(kontering.oppdragsperiode?.oppdrag?.sakId)
        writer.writeEndElement()

        writer.writeStartElement("delytelseId")
        writer.writeCharacters(kontering.oppdragsperiode?.delytelseId.toString())
        writer.writeEndElement()

        writer.writeStartElement("refDelytelseId")
        writer.writeCharacters(kontering.oppdragsperiode?.delytelseId.toString())
        writer.writeEndElement()

        writer.writeEndElement() // kontering-br10
    }

    private fun skrivStoppBatchBr99(writer: XMLStreamWriter, sum: BigDecimal, antall: Int) {
        writer.writeCharacters("\n")
        writer.writeStartElement("stopp-batch-br99")

        writer.writeStartElement("sumBelop")
        writer.writeCharacters(sum.toString())
        writer.writeEndElement()

        writer.writeStartElement("antallRecords")
        writer.writeCharacters(antall.toString())
        writer.writeEndElement()

        writer.writeEndElement() // stopp-batch-br99
    }

    private fun finnAlleSakerFraKonteringer(konteringer: List<Kontering>, påløp: Påløp): HashMap<String, ArrayList<Kontering>> {
        val sakerMap = HashMap<String, ArrayList<Kontering>>()
        val påløpKjøredato = påløp.kjøredato.toLocalDate()

        // Går igjennom alle konteringer, oppretter en liste for hvert oppdrag om det ikke allerede finnes og legger til konteringen i listen tilhørende tilknyttet oppdrag.
        konteringer.forEach { kontering ->

            // Om konteringen er knyttet til et oppdrag som har utsattTilDato senere enn nåværende tidspunkt så skal konteringen ikke være med i fila.
            if (kontering.oppdragsperiode?.oppdrag?.utsattTilDato?.isAfter(påløpKjøredato) == true) {
                return@forEach
            }
            // Om konteringen er forsøkt overført tidligere på noen andre konteringer i oppdragsperioden så
            if (kontering.oppdragsperiode?.konteringer?.any { it.overføringstidspunkt != null && it.behandlingsstatusOkTidspunkt == null } == true) {
                return@forEach
            }

            val saksnummer = kontering.oppdragsperiode?.oppdrag?.sakId!!
            var current = sakerMap[saksnummer]
            if (current == null) {
                current = ArrayList()
                sakerMap[saksnummer] = current
            }
            current.add(kontering)
        }
        return sakerMap
    }

    private fun medLyttere(lytterConsumer: Consumer<PåløpskjøringLytter>) = lyttere.forEach(lytterConsumer)
}
