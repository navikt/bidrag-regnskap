package no.nav.bidrag.regnskap.dto.vedtak

import no.nav.bidrag.behandling.felles.enums.VedtakType
import java.time.LocalDate

data class Hendelse(
    val type: String,
    val vedtakType: VedtakType,
    val kravhaverIdent: String?,
    val skyldnerIdent: String,
    val mottakerIdent: String,
    val sakId: String,
    val vedtakId: Int,
    val vedtakDato: LocalDate,
    val opprettetAv: String,
    val eksternReferanse: String?,
    val utsattTilDato: LocalDate?,
    val referanse: String? = null,
    val omgjørVedtakId: Int? = null,
    val periodeListe: List<Periode>
)
