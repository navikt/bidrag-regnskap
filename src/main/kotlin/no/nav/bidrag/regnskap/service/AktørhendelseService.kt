package no.nav.bidrag.regnskap.service

import no.nav.bidrag.regnskap.SECURE_LOGGER
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Type
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AktørhendelseService(
    private val persistenceService: PersistenceService,
) {

    @Transactional
    fun behandleAktoerHendelse(aktor: Aktor?) {
        val tidligereIdenter = aktor?.identifikatorer?.filter { ident ->
            ident.type == Type.FOLKEREGISTERIDENT && !ident.gjeldende
        }

        if (tidligereIdenter?.isNotEmpty() == true) {
            val gjeldendeIdent = aktor.identifikatorer?.singleOrNull { ident ->
                ident.type == Type.FOLKEREGISTERIDENT && ident.gjeldende
            }?.idnummer.toString()

            tidligereIdenter.forEach { tidligereIdent ->

                persistenceService.hentAlleMottakereMedIdent(tidligereIdent?.idnummer.toString()).forEach {
                    it.mottakerIdent = gjeldendeIdent
                    SECURE_LOGGER.info("Aktorhendelse: $tidligereIdent oppdatert til $gjeldendeIdent for oppdragId: ${it.oppdragId}")
                }

                persistenceService.hentAlleKravhavereMedIdent(tidligereIdent?.idnummer.toString()).forEach {
                    it.kravhaverIdent = gjeldendeIdent
                    SECURE_LOGGER.info("Aktorhendelse: $tidligereIdent oppdatert til $gjeldendeIdent for oppdragId: ${it.oppdragId}")
                }

                persistenceService.hentAlleSkyldnereMedIdent(tidligereIdent?.idnummer.toString()).forEach {
                    it.skyldnerIdent = gjeldendeIdent
                    SECURE_LOGGER.info("Aktorhendelse: $tidligereIdent oppdatert til $gjeldendeIdent for oppdragId: ${it.oppdragId}")
                }

                persistenceService.hentAlleGjelderMedIdent(tidligereIdent?.idnummer.toString()).forEach {
                    it.gjelderIdent = gjeldendeIdent
                    SECURE_LOGGER.info("Aktorhendelse: $tidligereIdent oppdatert til $gjeldendeIdent for oppdragId: ${it.oppdragId}")
                }
            }
        }
    }
}
