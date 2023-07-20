package no.nav.bidrag.regnskap.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AktoerhendelseService(
    private val persistenceService: PersistenceService
) {

    @Transactional
    fun behandleAktoerHendelse(hendelse: String) {
        val mottakerSomSkalEndres = persistenceService.hentAlleMottakereMedIdent(hendelse)
        if (mottakerSomSkalEndres.isNotEmpty()) {
            mottakerSomSkalEndres.forEach {
                it.mottakerIdent = hendelse
            }
        }

        val kravhaverSomSkalEndres = persistenceService.hentAlleKravhavereMedIdent(hendelse)
        if (kravhaverSomSkalEndres.isNotEmpty()) {
            kravhaverSomSkalEndres.forEach {
                it.kravhaverIdent = hendelse
            }
        }

        val skyldnerSomSkalEndres = persistenceService.hentAlleSkyldnereMedIdent(hendelse)
        if (skyldnerSomSkalEndres.isNotEmpty()) {
            skyldnerSomSkalEndres.forEach {
                it.skyldnerIdent = hendelse
            }
        }
        val gjelderSomSkalEndres = persistenceService.hentAlleGjelderMedIdent(hendelse)
        if (gjelderSomSkalEndres.isNotEmpty()) {
            gjelderSomSkalEndres.forEach {
                it.gjelderIdent = hendelse
            }
        }
    }
}
