package no.nav.bidrag.regnskap.exception

import org.springframework.http.HttpStatus
import java.lang.RuntimeException

class RestFeilResponseException(message: String, val httpStatus: HttpStatus): RuntimeException(message) {}