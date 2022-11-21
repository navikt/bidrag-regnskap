package no.nav.bidrag.regnskap.dto.vedtak

import no.nav.bidrag.behandling.felles.enums.VedtakType
import java.time.LocalDate

data class Hendelse(
  val engangsbelopId: Int? = null,
  val endretEngangsbelopId: Int? = null,
  val type: String,
  val vedtakType: VedtakType,
  val kravhaverIdent: String,
  val skyldnerIdent: String,
  val mottakerIdent: String,
  val sakId: String,
  val vedtakId: Int,
  val vedtakDato: LocalDate,
  val opprettetAv: String,
  val eksternReferanse: String?,
  val utsattTilDato: LocalDate?,
  val periodeListe: List<Periode>
)