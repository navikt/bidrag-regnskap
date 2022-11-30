package no.nav.bidrag.regnskap.persistence.entity

import org.hibernate.Hibernate
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity(name = "palop")
data class Påløp(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "palop_id")
  val påløpId: Int? = null,

  @Column(name = "kjoredato")
  val kjøredato: LocalDateTime,

  @Column(name = "fullfort_tidspunkt")
  var fullførtTidspunkt: LocalDateTime? = null,

  @Column(name = "for_periode")
  val forPeriode: String
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Påløp

    return påløpId != null && påløpId == other.påløpId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(påløpId = $påløpId , kjøredato = $kjøredato , fullførtTidspunkt = $fullførtTidspunkt , forPeriode = $forPeriode )"
  }
}
