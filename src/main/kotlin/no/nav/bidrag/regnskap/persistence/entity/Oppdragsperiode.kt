package no.nav.bidrag.regnskap.persistence.entity

import java.math.BigDecimal
import java.time.LocalDate
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity(name = "oppdragsperioder")
data class Oppdragsperiode(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "oppdragsperiode_id")
  val oppdragsperiodeId: Int? = null,

  @ManyToOne
  @JoinColumn(name = "oppdrag_id")
  val oppdrag: Oppdrag? = null,

  @Column(name = "sak_id")
  val sakId: String,

  @Column(name = "vedtak_id")
  val vedtakId: Int,

  @Column(name = "gjelder_ident")
  val gjelderIdent: String,

  @Column(name = "mottaker_ident")
  val mottakerIdent: String,

  @Column(name = "belop")
  val beløp: BigDecimal,

  @Column(name = "valuta")
  val valuta: String,

  @Column(name = "periode_fra")
  val periodeFra: LocalDate,

  @Column(name = "periode_til")
  val periodeTil: LocalDate?,

  @Column(name = "vedtaksdato")
  val vedtaksdato: LocalDate,

  @Column(name = "opprettet_av")
  val opprettetAv: String,

  @Column(name = "delytelses_id")
  val delytelseId: String,

  @Column(name = "aktiv_til")
  var aktivTil: LocalDate? = null,

  @OneToMany(mappedBy = "oppdragsperiode", cascade = [CascadeType.ALL])
  var konteringer: List<Kontering>? = null,
) {
  override fun toString(): String {
    return "Oppdragsperiode(" +
        "oppdragsperiodeId=$oppdragsperiodeId, " +
        "oppdragId=${oppdrag?.oppdragId}, " +
        "sakId=$sakId, " +
        "vedtakId=$vedtakId, " +
        "gjelderIdent=$gjelderIdent, " +
        "mottakerIdent=$mottakerIdent, " +
        "beløp=$beløp, " +
        "valuta=$valuta, " +
        "periodeFra=$periodeFra, " +
        "periodeTil=$periodeTil, " +
        "vedtaksdato=$vedtaksdato, " +
        "opprettetAv=$opprettetAv, " +
        "delytelseId=$delytelseId, " +
        "aktivTil=$aktivTil, " +
        "konteringer=$konteringer)"
  }
}
