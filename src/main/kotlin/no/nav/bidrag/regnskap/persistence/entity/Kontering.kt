package no.nav.bidrag.regnskap.persistence.entity

import java.time.LocalDateTime
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity(name = "konteringer")
data class Kontering(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "kontering_id")
  val konteringId: Int = 0,

  @ManyToOne
  @JoinColumn(name = "oppdragsperiode_id")
  val oppdragsperiode: Oppdragsperiode? = null,

  @Column(name = "transaksjonskode")
  val transaksjonskode: String,

  @Column(name = "overforingsperiode")
  val overføringsperiode: String,

  @Column(name = "overforingstidspunkt")
  var overføringstidspunkt: LocalDateTime? = null,

  @Column(name = "type")
  val type: String,

  @Column(name = "soknad_type")
  val søknadType: String,

  @Column(name = "sendt_i_palopsfil")
  var sendtIPåløpsfil: Boolean = false,

  @OneToMany(mappedBy = "kontering", cascade = [CascadeType.ALL])
  var overføringKontering: List<OverføringKontering>? = null
) {

  override fun toString(): String {
    return this::class.simpleName +
        "(konteringId = $konteringId , " +
        "oppdragsperiodeId = ${oppdragsperiode?.oppdragsperiodeId} , " +
        "transaksjonskode = $transaksjonskode , " +
        "overføringsperiode = $overføringsperiode , " +
        "overføringstidspunkt = $overføringstidspunkt , " +
        "type = $type , " +
        "søknadType = $søknadType , " +
        "sendtIPåløpsfil = $sendtIPåløpsfil )"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Kontering

    if (konteringId != other.konteringId) return false
    if (oppdragsperiode != other.oppdragsperiode) return false
    if (transaksjonskode != other.transaksjonskode) return false
    if (overføringsperiode != other.overføringsperiode) return false
    if (overføringstidspunkt != other.overføringstidspunkt) return false
    if (type != other.type) return false
    if (søknadType != other.søknadType) return false
    if (sendtIPåløpsfil != other.sendtIPåløpsfil) return false

    return true
  }

  override fun hashCode(): Int {
    var result = konteringId
    result = 31 * result + (oppdragsperiode?.hashCode() ?: 0)
    result = 31 * result + transaksjonskode.hashCode()
    result = 31 * result + overføringsperiode.hashCode()
    result = 31 * result + (overføringstidspunkt?.hashCode() ?: 0)
    result = 31 * result + type.hashCode()
    result = 31 * result + søknadType.hashCode()
    result = 31 * result + sendtIPåløpsfil.hashCode()
    return result
  }


}


