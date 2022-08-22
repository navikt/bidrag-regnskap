package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.consumer.BidragPersonConsumer
import no.nav.bidrag.regnskap.model.HentPersonResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExampleService(var bidragPersonConsumer: BidragPersonConsumer) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ExampleService::class.java)
    }

    fun hentDialogerForPerson(personId: String): HentPersonResponse? {
        LOGGER.info("Henter samtalereferat for person")
        return bidragPersonConsumer.hentPerson(personId)
    }
}