package no.nav.bidrag.regnskap.model

data class HentPersonResponse(
    val ident: String,
    val navn: String,
    val aktoerId: String
)