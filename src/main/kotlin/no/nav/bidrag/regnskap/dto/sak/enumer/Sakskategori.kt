package no.nav.bidrag.regnskap.dto.sak.enumer

enum class Sakskategori(
    private val beskrivelse: String,
    val gyldig: Boolean,
    behandlingstypeForvaltning: String?,
    behandlingstypeKlage: String,
    behandlingstypeSøknad: String,
    behandlingstype: String
) {
    N("Nasjonal", true, null, "ae0058", "ae0003", "ae0118"),
    U("Utland", true, "ae0106", "ae0108", "ae0110", "ae0106");

    override fun toString(): String {
        return beskrivelse
    }
}
