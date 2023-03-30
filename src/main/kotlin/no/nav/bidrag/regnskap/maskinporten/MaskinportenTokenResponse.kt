package no.nav.bidrag.regnskap.maskinporten

@SuppressWarnings("kotlin:S117")
data class MaskinportenTokenResponse(
    val access_token: String,
    val token_type: String?,
    val expires_in: Int?,
    val scope: String?
)
