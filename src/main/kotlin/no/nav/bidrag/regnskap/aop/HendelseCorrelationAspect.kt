package no.nav.bidrag.regnskap.aop

import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.CorrelationId.Companion.CORRELATION_ID_HEADER
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.*

@Component
@Aspect
class HendelseCorrelationAspect {

    @Before(value = "execution(* no.nav.bidrag.regnskap.hendelse.kafka.*.*.*(..))")
    fun leggTilHendelseSporingId(joinPoint: JoinPoint) {
        if (MDC.get(CORRELATION_ID_HEADER) != null) return

        // Fjern norske bokstaver fra metodenavn
        val methodName = joinPoint.signature.name.replace("[^A-Za-z0-9 ]".toRegex(), "")
        MDC.put(CORRELATION_ID_HEADER, CorrelationId.existing("${UUID.randomUUID().toString().subSequence(0, 8)}_$methodName").get())
    }

    @After(value = "execution(* no.nav.bidrag.regnskap.hendelse.kafka.*.*.*(..))")
    fun fjernHendelseSporingId(joinPoint: JoinPoint) {
        MDC.clear()
    }
}
