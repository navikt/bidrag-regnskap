package no.nav.bidrag.regnskap.persistence.entity

import java.time.LocalDate
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany

@Entity(name = "oppdragsperioder")
data class Oppdragsperiode(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "oppdragsperiode_id")
  val oppdragsperiodeId: Int? = null,

  @Column(name = "oppdrag_id")
  val oppdragId: Int? = null,

  @Column(name = "vedtak_id")
  val vedtakId: Int,

  @Column(name = "gjelder_ident")
  val gjelderIdent: String,

  @Column(name = "mottaker_ident")
  val mottakerIdent: String,

  @Column(name = "belop")
  val belop: Int,

  @Column(name = "valuta")
  val valuta: String,

  @Column(name = "periode_fra")
  val periodeFra: LocalDate,

  @Column(name = "periode_til")
  val periodeTil: LocalDate,

  @Column(name = "vedtaksdato")
  val vedtaksdato: LocalDate,

  @Column(name = "opprettet_av")
  val opprettetAv: String,

  @Column(name = "delytelses_id")
  val delytelseId: String,

  @Column(name = "aktiv")
  val aktiv: Boolean = true,

  @Column(name = "erstatter_periode")
  val erstatterPeriode: Int? = null,

  @Column(name = "tekst")
  val tekst: String?,

  @OneToMany(mappedBy = "oppdragsperiodeId", cascade = [CascadeType.ALL])
  val konteringer: List<Kontering>? = null,
)
