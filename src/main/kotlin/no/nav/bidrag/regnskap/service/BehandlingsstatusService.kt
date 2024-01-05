package no.nav.bidrag.regnskap.service

import no.nav.bidrag.domene.enums.regnskap.behandlingsstatus.Batchstatus
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.transport.regnskap.behandlingsstatus.BehandlingsstatusResponse
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BehandlingsstatusService(
    private val skattConsumer: SkattConsumer,
    private val persistenceService: PersistenceService,
) {

    fun hentKonteringerMedIkkeGodkjentBehandlingsstatus(sisteReferanser: List<String> = emptyList()): HashMap<String, MutableSet<Kontering>> {
        val map = HashMap<String, MutableSet<Kontering>>()
        hentKonteringer(sisteReferanser).forEach {
            map.getOrPut(it.sisteReferansekode!!) { mutableSetOf() }.add(it)
        }
        return map
    }

    fun hentBehandlingsstatusForIkkeGodkjenteKonteringer(
        konteringerSomIkkeHarFåttGodkjentBehandlingsstatus: HashMap<String, MutableSet<Kontering>>,
    ): HashMap<String, String> {
        val feilmeldinger = hashMapOf<String, String>()
        konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.forEach { (key, value) ->
            try {
                val behandlingsstatusResponse = hentBehandlingsstatus(key)
                val now = LocalDateTime.now()
                if (behandlingsstatusResponse.batchStatus == Batchstatus.Done) {
                    value.forEach { it.behandlingsstatusOkTidspunkt = now }
                } else {
                    feilmeldinger[key] = "Behandling av konteringer for batchuid $key har feilet: $behandlingsstatusResponse\n"
                }
            } catch (e: Exception) {
                feilmeldinger[key] = "Behandling av konteringer for batchuid $key har feilet og kastet exception!: ${e.message}\n"
            }
        }
        return feilmeldinger
    }

    fun hentBehandlingsstatusForIkkeGodkjenteKonteringerForReferansekode(sisteReferanser: List<String>): HashMap<String, String> {
        return hentBehandlingsstatusForIkkeGodkjenteKonteringer(hentKonteringerMedIkkeGodkjentBehandlingsstatus(sisteReferanser))
    }

    private fun hentKonteringer(sisteReferanser: List<String>): List<Kontering> {
        return if (sisteReferanser.isEmpty()) {
            persistenceService.hentAlleKonteringerUtenBehandlingsstatusOk()
        } else {
            persistenceService.hentKonteringerUtenBehandlingsstatusOkForReferansekode(sisteReferanser)
        }
    }

    private fun hentBehandlingsstatus(batchUid: String): BehandlingsstatusResponse {
        val behandlingsstatus = skattConsumer.sjekkBehandlingsstatus(batchUid)
        return behandlingsstatus.body
            ?: error("Sjekk av behandlingsstatus feilet for batchUid: $batchUid! Feilkode: ${behandlingsstatus.statusCode}")
    }
}
