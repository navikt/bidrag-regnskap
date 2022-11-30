package no.nav.bidrag.regnskap.persistence.entity

import org.hibernate.Hibernate
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
  val konteringId: Int? = null,

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
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Kontering

    return konteringId != null && konteringId == other.konteringId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
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
}


