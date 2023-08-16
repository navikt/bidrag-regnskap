package no.nav.bidrag.regnskap.util

import no.nav.bidrag.commons.util.SjekkForNyIdent
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class IdentUtils {

    companion object {
        const val NAV_TSS_IDENT = "80000345435"
    }

    @Cacheable(value = ["bidrag-regnskap_hentNyesteIdent_cache"], key = "#ident")
    fun hentNyesteIdent(@SjekkForNyIdent ident: String): String {
        return if (ident == "NAV") NAV_TSS_IDENT else ident
    }
}
