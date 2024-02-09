package no.nav.bidrag.regnskap.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.dto.vedtak.Periode
import no.nav.bidrag.regnskap.util.IdentUtils
import no.nav.bidrag.transport.behandling.vedtak.Engangsbeløp
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(VedtakshendelseService::class.java)
private val objectMapper =
    ObjectMapper().registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

@Service
class VedtakshendelseService(
    private val oppdragService: OppdragService,
    private val kravService: KravService,
    private val persistenceService: PersistenceService,
    private val identUtils: IdentUtils,
) {

    fun behandleHendelse(hendelse: String): List<Int> {
        val vedtakHendelse = mapVedtakHendelse(hendelse)

        LOGGER.info("Behandler vedakHendelse for vedtakid: ${vedtakHendelse.id}")
        SECURE_LOGGER.info("Behandler vedtakHendelse: $vedtakHendelse\nVedtakhendelse som json string: $hendelse")

        val opprettedeOppdrag = mutableListOf<Int>()

        vedtakHendelse.stønadsendringListe?.forEach { stønadsendring ->
            opprettOppdragForStønadsending(vedtakHendelse, stønadsendring)?.let {
                opprettedeOppdrag.add(it)
            }
        }

        vedtakHendelse.engangsbeløpListe?.forEach { engangsbelop ->
            opprettOppdragForEngangsbeløp(vedtakHendelse, engangsbelop)?.let {
                opprettedeOppdrag.add(it)
            }
        }

        return opprettedeOppdrag
    }

    fun mapVedtakHendelse(hendelse: String): VedtakHendelse {
        return try {
            objectMapper.readValue(hendelse, VedtakHendelse::class.java)
        } finally {
            SECURE_LOGGER.debug("Leser hendelse: {}", hendelse)
        }
    }

    private fun opprettOppdragForStønadsending(vedtakHendelse: VedtakHendelse, stønadsendring: Stønadsendring): Int? {
        LOGGER.debug("Oppretter oppdrag for stønadendring.")

        if (erInnkrevingOgEndring(stønadsendring.innkreving, stønadsendring.beslutning)) {
            val hendelse = Hendelse(
                type = stønadsendring.type.name,
                vedtakType = vedtakHendelse.type,
                kravhaverIdent = identUtils.hentNyesteIdent(stønadsendring.kravhaver.verdi),
                skyldnerIdent = identUtils.hentNyesteIdent(stønadsendring.skyldner.verdi),
                mottakerIdent = identUtils.hentNyesteIdent(stønadsendring.mottaker.verdi),
                sakId = stønadsendring.sak.verdi,
                vedtakId = vedtakHendelse.id,
                vedtakDato = vedtakHendelse.vedtakstidspunkt.toLocalDate(),
                opprettetAv = vedtakHendelse.opprettetAv,
                enhetsnummer = vedtakHendelse.enhetsnummer,
                eksternReferanse = stønadsendring.eksternReferanse,
                utsattTilDato = vedtakHendelse.innkrevingUtsattTilDato,
                omgjørVedtakId = stønadsendring.omgjørVedtakId,
                periodeListe = mapPeriodelisteTilDomene(stønadsendring.periodeListe),
            )
            return oppdragService.lagreHendelse(hendelse)
        }
        return null
    }

    private fun erInnkrevingOgEndring(innkreving: Innkrevingstype, beslutningstype: Beslutningstype): Boolean {
        return innkreving == Innkrevingstype.MED_INNKREVING && beslutningstype == Beslutningstype.ENDRING
    }

    private fun mapPeriodelisteTilDomene(periodeListe: List<no.nav.bidrag.transport.behandling.vedtak.Periode>): List<Periode> {
        return periodeListe.map { periode ->
            Periode(
                beløp = periode.beløp,
                valutakode = periode.valutakode,
                periodeFomDato = periode.periode.toDatoperiode().fom,
                periodeTilDato = periode.periode.toDatoperiode().til,
                delytelsesId = periode.delytelseId?.let { Integer.valueOf(it) },
            )
        }
    }

    private fun opprettOppdragForEngangsbeløp(vedtakHendelse: VedtakHendelse, engangsbeløp: Engangsbeløp): Int? {
        LOGGER.debug("Oppretter oppdrag for engangsbeløp.")

        if (erInnkrevingOgEndring(engangsbeløp.innkreving, engangsbeløp.beslutning)) {
            val hendelse = Hendelse(
                type = engangsbeløp.type.name,
                vedtakType = vedtakHendelse.type,
                kravhaverIdent = identUtils.hentNyesteIdent(engangsbeløp.kravhaver.verdi),
                skyldnerIdent = identUtils.hentNyesteIdent(engangsbeløp.skyldner.verdi),
                mottakerIdent = identUtils.hentNyesteIdent(engangsbeløp.mottaker.verdi),
                sakId = engangsbeløp.sak.verdi,
                vedtakId = vedtakHendelse.id,
                vedtakDato = vedtakHendelse.vedtakstidspunkt.toLocalDate(),
                opprettetAv = vedtakHendelse.opprettetAv,
                enhetsnummer = vedtakHendelse.enhetsnummer,
                eksternReferanse = engangsbeløp.eksternReferanse,
                utsattTilDato = vedtakHendelse.innkrevingUtsattTilDato,
                referanse = engangsbeløp.referanse,
                omgjørVedtakId = engangsbeløp.omgjørVedtakId,
                periodeListe = listOf(
                    Periode(
                        periodeFomDato = vedtakHendelse.vedtakstidspunkt.toLocalDate().withDayOfMonth(1),
                        periodeTilDato = vedtakHendelse.vedtakstidspunkt.toLocalDate().withDayOfMonth(1).plusMonths(1),
                        beløp = engangsbeløp.beløp,
                        valutakode = engangsbeløp.valutakode,
                        delytelsesId = engangsbeløp.delytelseId?.let { Integer.valueOf(it) },
                    ),
                ),
            )
            return oppdragService.lagreHendelse(hendelse, true)
        }
        return null
    }

    fun sendKrav(oppdragIdListe: List<Int>) {
        if (harAktiveDriftAvvik()) {
            LOGGER.info("Det finnes aktive driftsavvik. Starter derfor ikke overføring av konteringer for oppdrag: $oppdragIdListe.")
            return
        } else if (erVedlikeholdsmodusPåslått()) {
            LOGGER.info("Vedlikeholdsmodus er påslått! Starter derfor ikke overføring av kontering for oppdrag: $oppdragIdListe.")
            return
        }
        kravService.sendKrav(oppdragIdListe)
    }

    private fun erVedlikeholdsmodusPåslått(): Boolean {
        return kravService.erVedlikeholdsmodusPåslått()
    }

    private fun harAktiveDriftAvvik(): Boolean {
        return persistenceService.harAktivtDriftsavvik()
    }
}
