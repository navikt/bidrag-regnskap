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
  val oppdragId: Int = 0,

  @Column(name = "stonad_type")
  val stønadType: String,

  @Column(name = "vedtak_type")
  var vedtakType: String,

  @Column(name = "sak_id")
  val sakId: String,

  @Column(name = "kravhaver_ident")
  val kravhaverIdent: String? = null,

  @Column(name = "skyldner_ident")
  val skyldnerIdent: String,

  @Column(name = "ekstern_referanse")
  val eksternReferanse: String? = null,

  @Column(name = "utsatt_til_dato")
  var utsattTilDato: LocalDate? = null,

  @Column(name = "endret_tidspunkt")
  @Version
  var endretTidspunkt: LocalDateTime? = null,

  @Column(name = "engangsbelop_id")
  var engangsbeløpId: Int? = null,

  @OneToMany(mappedBy = "oppdrag", cascade = [CascadeType.ALL])
  var oppdragsperioder: List<Oppdragsperiode>? = null
) {

  override fun toString(): String {
    return this::class.simpleName +
        "(oppdragId = $oppdragId , " +
        "stønadType = $stønadType , " +
        "vedtakType = $vedtakType , " +
        "sakId = $sakId , " +
        "kravhaverIdent = $kravhaverIdent , " +
        "skyldnerIdent = $skyldnerIdent , " +
        "eksternReferanse = $eksternReferanse , " +
        "utsattTilDato = $utsattTilDato , " +
        "endretTidspunkt = $endretTidspunkt , " +
        "engangsbeløpId = $engangsbeløpId )"
  }
}
