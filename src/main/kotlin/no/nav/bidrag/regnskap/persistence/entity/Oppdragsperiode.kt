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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Oppdragsperiode

    if (oppdragsperiodeId != other.oppdragsperiodeId) return false
    if (oppdrag != other.oppdrag) return false
    if (vedtakId != other.vedtakId) return false
    if (gjelderIdent != other.gjelderIdent) return false
    if (mottakerIdent != other.mottakerIdent) return false
    if (beløp != other.beløp) return false
    if (valuta != other.valuta) return false
    if (periodeFra != other.periodeFra) return false
    if (periodeTil != other.periodeTil) return false
    if (vedtaksdato != other.vedtaksdato) return false
    if (opprettetAv != other.opprettetAv) return false
    if (delytelseId != other.delytelseId) return false
    if (aktivTil != other.aktivTil) return false

    return true
  }

  override fun hashCode(): Int {
    var result = oppdragsperiodeId
    result = 31 * result + (oppdrag?.hashCode() ?: 0)
    result = 31 * result + vedtakId
    result = 31 * result + gjelderIdent.hashCode()
    result = 31 * result + mottakerIdent.hashCode()
    result = 31 * result + beløp.hashCode()
    result = 31 * result + valuta.hashCode()
    result = 31 * result + periodeFra.hashCode()
    result = 31 * result + (periodeTil?.hashCode() ?: 0)
    result = 31 * result + vedtaksdato.hashCode()
    result = 31 * result + opprettetAv.hashCode()
    result = 31 * result + (delytelseId ?: 0)
    result = 31 * result + (aktivTil?.hashCode() ?: 0)
    return result
  }


}
