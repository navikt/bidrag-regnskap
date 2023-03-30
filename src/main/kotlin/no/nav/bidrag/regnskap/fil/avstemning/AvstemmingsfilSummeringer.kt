package no.nav.bidrag.regnskap.fil.avstemning

import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import java.math.BigDecimal

data class AvstemmingsfilSummeringer(
    val transaksjonskode: Transaksjonskode,
    var sum: BigDecimal,
    var antallKonteringer: Int,
    val fradragEllerTillegg: String? = if (transaksjonskode.korreksjonskode == null) "F" else "T"
)
