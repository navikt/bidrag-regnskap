package no.nav.bidrag.regnskap.persistence.entity

import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.DynamicInsert
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
@DynamicInsert
data class Oppdragsperiode(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "oppdragsperiode_id")
  val oppdragsperiodeId: Int = 0,

  @ManyToOne
  @JoinColumn(name = "oppdrag_id")
  val oppdrag: Oppdrag? = null,

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
  @ColumnDefault("nextval('delytelsesId_seq')")
  val delytelseId: Int?,

  @Column(name = "aktiv_til")
  var aktivTil: LocalDate? = null,

  @OneToMany(mappedBy = "oppdragsperiode", cascade = [CascadeType.ALL])
  var konteringer: List<Kontering>? = null,
) {

  override fun toString(): String {
    return this::class.simpleName +
        "(oppdragsperiodeId = $oppdragsperiodeId , " +
        "oppdragId = ${oppdrag?.oppdragId} , " +
        "vedtakId = $vedtakId , " +
        "gjelderIdent = $gjelderIdent , " +
        "mottakerIdent = $mottakerIdent , " +
        "beløp = $beløp , " +
        "valuta = $valuta , " +
        "periodeFra = $periodeFra , " +
        "periodeTil = $periodeTil , " +
        "vedtaksdato = $vedtaksdato , " +
        "opprettetAv = $opprettetAv , " +
        "delytelseId = $delytelseId , " +
        "aktivTil = $aktivTil )"
  }
}
