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

  @Column(name = "referanse")
  val referanse: String? = null,

  @Column(name = "utsatt_til_dato")
  val utsattTilDato: LocalDate? = null,

  @Column(name = "sist_oversendte_periode")
  val sistOversendtePeriode: String? = null,

  @Column(name = "endret_tidspunkt")
  val endretTidspunkt: LocalDateTime? = null,

  @OneToMany(mappedBy = "oppdrag", cascade = [CascadeType.ALL])
  var oppdragsperioder: List<Oppdragsperiode>? = null
) {
  override fun toString(): String {
    return ""
  }
}
