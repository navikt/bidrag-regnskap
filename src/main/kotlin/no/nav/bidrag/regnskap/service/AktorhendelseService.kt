package no.nav.bidrag.regnskap.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AktorhendelseService(
    private val persistenceService: PersistenceService
) {

    @Transactional
    fun behandleAktoerHendelse(ident: String) {
        persistenceService.hentAlleMottakereMedIdent(ident).forEach {
            it.mottakerIdent = ident
        }

        persistenceService.hentAlleKravhavereMedIdent(ident).forEach {
            it.kravhaverIdent = ident
        }

        persistenceService.hentAlleSkyldnereMedIdent(ident).forEach {
            it.skyldnerIdent = ident
        }

        persistenceService.hentAlleGjelderMedIdent(ident).forEach {
            it.gjelderIdent = ident
        }
    }
}
