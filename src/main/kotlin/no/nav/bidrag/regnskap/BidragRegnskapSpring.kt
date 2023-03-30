package no.nav.bidrag.regnskap

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.scheduling.annotation.EnableAsync

const val PROFILE_NAIS = "nais"
val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLogger")

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class])
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
@EnableAsync
class BidragRegnskap

fun main(args: Array<String>) {
    val app = SpringApplication(BidragRegnskap::class.java)
    app.setAdditionalProfiles(if (args.isEmpty()) PROFILE_NAIS else args[0])
    app.run(*args)
}
