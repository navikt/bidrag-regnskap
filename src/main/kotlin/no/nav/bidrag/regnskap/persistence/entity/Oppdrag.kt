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
import javax.persistence.OrderBy
import javax.persistence.Version

@Entity(name = "oppdrag")
data class Oppdrag(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "oppdrag_id")
  val oppdragId: Int = 0,

  @Column(name = "stonad_type")
  val stønadType: String,

  @Column(name = "sak_id")
  val sakId: String,

  @Column(name = "kravhaver_ident")
  val kravhaverIdent: String? = null,

  @Column(name = "skyldner_ident")
  val skyldnerIdent: String,

  @Column(name = "utsatt_til_dato")
  var utsattTilDato: LocalDate? = null,

  @Column(name = "endret_tidspunkt")
  @Version
  var endretTidspunkt: LocalDateTime? = null,

  @Column(name = "engangsbelop_id")
  var engangsbeløpId: Int? = null,

  @OneToMany(mappedBy = "oppdrag", cascade = [CascadeType.ALL])
  @OrderBy("oppdragsperiodeId")
  var oppdragsperioder: List<Oppdragsperiode> = emptyList()
) {

  override fun toString(): String {
    return this::class.simpleName +
        "(oppdragId = $oppdragId , " +
        "stønadType = $stønadType , " +
        "sakId = $sakId , " +
        "kravhaverIdent = $kravhaverIdent , " +
        "skyldnerIdent = $skyldnerIdent , " +
        "utsattTilDato = $utsattTilDato , " +
        "endretTidspunkt = $endretTidspunkt , " +
        "engangsbeløpId = $engangsbeløpId )"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Oppdrag

    if (oppdragId != other.oppdragId) return false
    if (stønadType != other.stønadType) return false
    if (sakId != other.sakId) return false
    if (kravhaverIdent != other.kravhaverIdent) return false
    if (skyldnerIdent != other.skyldnerIdent) return false
    if (utsattTilDato != other.utsattTilDato) return false
    if (endretTidspunkt != other.endretTidspunkt) return false
    if (engangsbeløpId != other.engangsbeløpId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = oppdragId
    result = 31 * result + stønadType.hashCode()
    result = 31 * result + sakId.hashCode()
    result = 31 * result + (kravhaverIdent?.hashCode() ?: 0)
    result = 31 * result + skyldnerIdent.hashCode()
    result = 31 * result + (utsattTilDato?.hashCode() ?: 0)
    result = 31 * result + (endretTidspunkt?.hashCode() ?: 0)
    result = 31 * result + (engangsbeløpId ?: 0)
    return result
  }


}
