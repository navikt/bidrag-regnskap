package no.nav.bidrag.regnskap.aop

import no.nav.bidrag.regnskap.maskinporten.MaskinportenClientException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpStatusCodeException

@RestControllerAdvice
class DefaultRestControllerAdvice {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(DefaultRestControllerAdvice::class.java)
    }

    @ResponseBody
    @ExceptionHandler(Exception::class)
    fun handleOtherExceptions(exception: Exception): ResponseEntity<*> {
        LOGGER.warn("Det skjedde en ukjent feil", exception)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, "Det skjedde en ukjent feil: ${exception.message}")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleUnauthorizedException(exception: JwtTokenUnauthorizedException): ResponseEntity<*> {
        LOGGER.warn("Ugyldig eller manglende sikkerhetstoken", exception)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WARNING, "Ugyldig eller manglende sikkerhetstoken").build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleFlereAktivePerioderException(exception: HttpStatusCodeException): ResponseEntity<*> {
        LOGGER.error(exception.message)
        return ResponseEntity.status(exception.statusCode).body(exception.message)
    }

    @ResponseBody
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(exception: java.util.NoSuchElementException): ResponseEntity<*> {
        LOGGER.info("Fant ingen gyldig verdi.", exception)
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(MaskinportenClientException::class)
    fun handleMaskinportenClientExcpetion(exception: MaskinportenClientException): ResponseEntity<*> {
        LOGGER.info("Noe gikk galt ved kall til maskinporten.", exception)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(exception.message)
    }

}