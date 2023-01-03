package no.nav.bidrag.regnskap.dto.sak.enumer

enum class Rolletype(val beskrivelse: String, val gyldig: Boolean) {
  BA("Barn", true),
  BM("Bidragsmottaker", true),
  BP("Bidragspliktig", true),
  FR("Feilregistrert", true),
  RM("ReellMottaker", true);
}