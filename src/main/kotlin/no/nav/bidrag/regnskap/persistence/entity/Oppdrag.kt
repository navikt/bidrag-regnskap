package no.nav.bidrag.regnskap.persistence.entity

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
  val stonadType: String,

  @Column(name = "kravhaver_ident")
  val kravhaverIdent: String? = null,

  @Column(name = "skyldner_ident")
  val skyldnerIdent: String,

  @Column(name = "ekstern_referanse")
  val eksternReferanse: String? = null,

  @Column(name = "utsatt_til_dato")
  val utsattTilDato: LocalDate? = null,

  @Column(name = "sist_oversendte_periode")
  var sistOversendtePeriode: String? = null,

  @Column(name = "endret_tidspunkt")
  @Version
  var endretTidspunkt: LocalDateTime? = null,

  @Column(name = "engangsbelop_id")
  val engangsbelopId: Int? = null,

  @OneToMany(mappedBy = "oppdrag", cascade = [CascadeType.ALL])
  var oppdragsperioder: List<Oppdragsperiode>? = null
) {
  override fun toString(): String {
    return "Oppdrag(" +
        "oppdragId=$oppdragId, " +
        "stonadType='$stonadType, " +
        "kravhaverIdent=$kravhaverIdent, " +
        "skyldnerIdent=$skyldnerIdent, " +
        "eksternReferanse=$eksternReferanse, " +
        "utsattTilDato=$utsattTilDato, " +
        "sistOversendtePeriode=$sistOversendtePeriode, " +
        "endretTidspunkt=$endretTidspunkt, " +
        "engangsbelopId=$engangsbelopId, " +
        "oppdragsperioder=$oppdragsperioder)"
  }
}
