package no.nav.bidrag.regnskap.dto.vedtak

import java.time.LocalDate

data class UtsatteOgFeiledeVedtak(
    val utsatteVedtak: List<UtsatteVedtak>,
    val ikkeOversendteVedtak: List<IkkeOversendteVedtak>,
    val feiledeVedtak: List<FeiledeVedtak>,
)

data class IkkeOversendteVedtak(
    val vedtakId: Int,
)

data class UtsatteVedtak(
    val vedtakId: Int,
    val utsattTil: LocalDate,
)
data class FeiledeVedtak(
    val vedtakId: Int,
    val feilmelding: String?,
)
