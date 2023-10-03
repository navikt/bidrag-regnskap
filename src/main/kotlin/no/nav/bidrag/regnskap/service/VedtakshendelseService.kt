package no.nav.bidrag.regnskap.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.bidrag.domain.enums.Innkreving
import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.dto.vedtak.Periode
import no.nav.bidrag.regnskap.util.IdentUtils
import no.nav.bidrag.transport.behandling.vedtak.Engangsbelop
import no.nav.bidrag.transport.behandling.vedtak.Stonadsendring
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
    private val identUtils: IdentUtils
) {

    fun behandleHendelse(hendelse: String): List<Int> {
        val vedtakHendelse = mapVedtakHendelse(hendelse)

        LOGGER.info("Behandler vedakHendelse for vedtakid: ${vedtakHendelse.id}")
        SECURE_LOGGER.info("Behandler vedtakHendelse: $vedtakHendelse\nVedtakhendelse som json string: $hendelse")

        val opprettedeOppdrag = mutableListOf<Int>()

        vedtakHendelse.stonadsendringListe?.forEach { stønadsendring ->
            opprettOppdragForStonadsending(vedtakHendelse, stønadsendring)?.let {
                opprettedeOppdrag.add(it)
            }
        }

        vedtakHendelse.engangsbelopListe?.forEach { engangsbelop ->
            opprettOppdragForEngangsbelop(vedtakHendelse, engangsbelop)?.let {
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

    private fun opprettOppdragForStonadsending(vedtakHendelse: VedtakHendelse, stonadsendring: Stonadsendring): Int? {
        LOGGER.debug("Oppretter oppdrag for stonadendring.")

        if (erInnkrevingOgEndring(stonadsendring.innkreving, stonadsendring.endring)) {
            val hendelse = Hendelse(
                type = stonadsendring.type.name,
                vedtakType = vedtakHendelse.type,
                kravhaverIdent = identUtils.hentNyesteIdent(stonadsendring.kravhaverId),
                skyldnerIdent = identUtils.hentNyesteIdent(stonadsendring.skyldnerId),
                mottakerIdent = identUtils.hentNyesteIdent(stonadsendring.mottakerId),
                sakId = stonadsendring.sakId,
                vedtakId = vedtakHendelse.id,
                vedtakDato = vedtakHendelse.vedtakTidspunkt.toLocalDate(),
                opprettetAv = vedtakHendelse.opprettetAv,
                eksternReferanse = stonadsendring.eksternReferanse,
                utsattTilDato = vedtakHendelse.utsattTilDato,
                omgjørVedtakId = stonadsendring.omgjorVedtakId,
                periodeListe = mapPeriodelisteTilDomene(stonadsendring.periodeListe)
            )
            return oppdragService.lagreHendelse(hendelse)
        }
        return null
    }

    private fun erInnkrevingOgEndring(innkreving: Innkreving, endring: Boolean): Boolean {
        return innkreving == Innkreving.JA && endring
    }

    private fun mapPeriodelisteTilDomene(periodeListe: List<no.nav.bidrag.transport.behandling.vedtak.Periode>): List<Periode> {
        return periodeListe.map { periode ->
            Periode(
                beløp = periode.belop,
                valutakode = periode.valutakode,
                periodeFomDato = periode.fomDato,
                periodeTilDato = periode.tilDato,
                delytelsesId = periode.delytelseId?.let { Integer.valueOf(it) }
            )
        }
    }

    private fun opprettOppdragForEngangsbelop(vedtakHendelse: VedtakHendelse, engangsbelop: Engangsbelop): Int? {
        LOGGER.debug("Oppretter oppdrag for engangsbeløp.")

        if (erInnkrevingOgEndring(engangsbelop.innkreving, engangsbelop.endring)) {
            val hendelse = Hendelse(
                type = engangsbelop.type.name,
                vedtakType = vedtakHendelse.type,
                kravhaverIdent = identUtils.hentNyesteIdent(engangsbelop.kravhaverId),
                skyldnerIdent = identUtils.hentNyesteIdent(engangsbelop.skyldnerId),
                mottakerIdent = identUtils.hentNyesteIdent(engangsbelop.mottakerId),
                sakId = engangsbelop.sakId,
                vedtakId = vedtakHendelse.id,
                vedtakDato = vedtakHendelse.vedtakTidspunkt.toLocalDate(),
                opprettetAv = vedtakHendelse.opprettetAv,
                eksternReferanse = engangsbelop.eksternReferanse,
                utsattTilDato = vedtakHendelse.utsattTilDato,
                referanse = engangsbelop.referanse,
                omgjørVedtakId = engangsbelop.omgjorVedtakId,
                periodeListe = listOf(
                    Periode(
                        periodeFomDato = vedtakHendelse.vedtakTidspunkt.toLocalDate().withDayOfMonth(1),
                        periodeTilDato = vedtakHendelse.vedtakTidspunkt.toLocalDate().withDayOfMonth(1).plusMonths(1),
                        beløp = engangsbelop.belop,
                        valutakode = engangsbelop.valutakode,
                        delytelsesId = engangsbelop.delytelseId?.let { Integer.valueOf(it) }
                    )
                )
            )
            return oppdragService.lagreHendelse(hendelse)
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
