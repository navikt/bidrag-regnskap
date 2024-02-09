package no.nav.bidrag.regnskap.dto.patch

import java.time.LocalDate

data class OppdaterUtsattTilDatoRequest(
    val oppdragsid: Int,
    val nyUtsattTilDato: LocalDate?,
)
