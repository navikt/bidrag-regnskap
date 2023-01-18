package no.nav.bidrag.regnskap.persistence.entity

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
  val påløpId: Int = 0,

  @Column(name = "kjoredato")
  val kjøredato: LocalDateTime,

  @Column(name = "fullfort_tidspunkt")
  var fullførtTidspunkt: LocalDateTime? = null,

  @Column(name = "for_periode")
  val forPeriode: String
) {

  override fun toString(): String {
    return this::class.simpleName + "(påløpId = $påløpId , kjøredato = $kjøredato , fullførtTidspunkt = $fullførtTidspunkt , forPeriode = $forPeriode )"
  }
}
