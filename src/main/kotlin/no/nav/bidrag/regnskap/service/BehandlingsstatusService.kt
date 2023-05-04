package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.dto.behandlingsstatus.Batchstatus
import no.nav.bidrag.regnskap.dto.behandlingsstatus.BehandlingsstatusResponse
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BehandlingsstatusService(
    private val skattConsumer: SkattConsumer,
    private val persistenceService: PersistenceService
) {

    fun hentKonteringerMedIkkeGodkjentBehandlingsstatus(): HashMap<String, MutableSet<Kontering>> {
        val map = HashMap<String, MutableSet<Kontering>>()
        persistenceService.hentAlleKonteringerUtenBehandlingsstatusOk().forEach {
            map.getOrPut(it.sisteReferansekode!!) { mutableSetOf() }.add(it)
        }
        return map
    }

    fun hentBehandlingsstatusForIkkeGodkjenteKonteringer(konteringerSomIkkeHarFåttGodkjentBehandlingsstatus: java.util.HashMap<String, MutableSet<Kontering>>): HashMap<String, String> {
        val feilmeldinger = hashMapOf<String, String>()
        konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.forEach { (key, value) ->
            val behandlingsstatusResponse = hentBehandlingsstatus(key)
            val now = LocalDateTime.now()
            if (behandlingsstatusResponse.batchStatus == Batchstatus.Done) {
                value.forEach { it.behandlingsstatusOkTidspunkt = now }
            } else {
                feilmeldinger[key] = {
                    "Behandling av konteringer for batchuid $key har feilet. " +
                        "\nFeilmedling: $behandlingsstatusResponse"
                }.toString()
            }
        }
        return feilmeldinger
    }

    private fun hentBehandlingsstatus(batchUid: String): BehandlingsstatusResponse {
        val behandlingsstatus = skattConsumer.sjekkBehandlingsstatus(batchUid)
        return behandlingsstatus.body ?: error("Sjekk av behandlingsstatus feilet for batchUid: $batchUid! Feilkode: ${behandlingsstatus.statusCode}")
    }
}
