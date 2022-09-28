package no.nav.bidrag.regnskap.exception

import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import java.lang.RuntimeException

class OppdragFinnesAlleredeException(message: String, val oppdrag: Oppdrag): RuntimeException(message) {
}