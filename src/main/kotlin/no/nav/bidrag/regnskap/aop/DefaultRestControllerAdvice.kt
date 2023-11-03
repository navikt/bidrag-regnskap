package no.nav.bidrag.regnskap.aop

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.security.maskinporten.MaskinportenClientException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpStatusCodeException

private val LOGGER = KotlinLogging.logger {}

@RestControllerAdvice
class DefaultRestControllerAdvice {

    @ResponseBody
    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleHttpClientErrorException(exception: HttpStatusCodeException): ResponseEntity<*> {
        val errorMessage = getErrorMessage(exception)
        LOGGER.warn(exception) { errorMessage }
        return ResponseEntity.status(exception.statusCode).header(HttpHeaders.WARNING, errorMessage).build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleUnauthorizedException(exception: JwtTokenUnauthorizedException?): ResponseEntity<*> {
        LOGGER.warn(exception) { "Ugyldig eller manglende sikkerhetstoken" }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.WARNING, "Ugyldig eller manglende sikkerhetstoken").build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<*> {
        LOGGER.warn(exception) { "Det skjedde en ukjent feil" }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, "Det skjedde en ukjent feil: " + exception.message).build<Any>()
    }

    private fun getErrorMessage(exception: HttpStatusCodeException): String {
        val errorMessage = StringBuilder()
        errorMessage.append("Det skjedde en feil ved kall mot ekstern tjeneste: ")
        exception.responseHeaders?.get(HttpHeaders.WARNING)?.firstOrNull()?.let { errorMessage.append(it) }
        if (exception.statusText.isNotEmpty()) {
            errorMessage.append(exception.statusText)
        }
        if (exception.message?.isNotEmpty() == true) {
            errorMessage.append(exception.message)
        }
        return errorMessage.toString()
    }

    @ResponseBody
    @ExceptionHandler(MaskinportenClientException::class)
    fun handleMaskinportenClientExcpetion(exception: MaskinportenClientException): ResponseEntity<*> {
        LOGGER.error(exception) { "Noe gikk galt ved kall til maskinporten: ${exception.message}}" }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(exception.message)
    }
}
