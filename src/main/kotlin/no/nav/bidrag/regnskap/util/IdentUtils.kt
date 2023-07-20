package no.nav.bidrag.regnskap.util

import org.springframework.stereotype.Component

@Component
class IdentUtils {

    companion object {
        const val NAV_TSS_IDENT = "80000345435"
    }

    fun hentNyesteIdent(@SjekkForNyIdent ident: String): String {
        return if (ident == "NAV") NAV_TSS_IDENT else ident
    }
}
