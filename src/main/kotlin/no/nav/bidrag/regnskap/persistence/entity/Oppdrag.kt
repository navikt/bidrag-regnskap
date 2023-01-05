package no.nav.bidrag.regnskap.persistence.entity

import org.hibernate.Hibernate
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Version

@Entity(name = "oppdrag")
data class Oppdrag(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "oppdrag_id")
  val oppdragId: Int? = null,

  @Column(name = "stonad_type")
  val stønadType: String,

  @Column(name = "vedtak_type")
  var vedtakType: String,

  @Column(name = "kravhaver_ident")
  val kravhaverIdent: String? = null,

  @Column(name = "skyldner_ident")
  val skyldnerIdent: String,

  @Column(name = "ekstern_referanse")
  val eksternReferanse: String? = null,

  @Column(name = "utsatt_til_dato")
  var utsattTilDato: LocalDate? = null,

  @Column(name = "sist_oversendte_periode")
  var sistOversendtePeriode: String? = null,

  @Column(name = "endret_tidspunkt")
  @Version
  var endretTidspunkt: LocalDateTime? = null,

  @Column(name = "engangsbelop_id")
  var engangsbeløpId: Int? = null,

  @OneToMany(mappedBy = "oppdrag", cascade = [CascadeType.ALL])
  var oppdragsperioder: List<Oppdragsperiode>? = null
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Oppdrag

    return oppdragId != null && oppdragId == other.oppdragId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName +
        "(oppdragId = $oppdragId , " +
        "stønadType = $stønadType , " +
        "vedtakType = $vedtakType , " +
        "kravhaverIdent = $kravhaverIdent , " +
        "skyldnerIdent = $skyldnerIdent , " +
        "eksternReferanse = $eksternReferanse , " +
        "utsattTilDato = $utsattTilDato , " +
        "sistOversendtePeriode = $sistOversendtePeriode , " +
        "endretTidspunkt = $endretTidspunkt , " +
        "engangsbeløpId = $engangsbeløpId )"
  }
}
