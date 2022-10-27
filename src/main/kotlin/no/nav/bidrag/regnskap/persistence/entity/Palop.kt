package no.nav.bidrag.regnskap.persistence.entity

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity(name = "palop")
data class Palop(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "palop_id")
  val palopId: Int? = null,

  @Column(name = "kjoredato")
  val kjoredato: LocalDateTime,

  @Column(name = "fullfort_tidspunkt")
  val fullfortTidspunkt: LocalDateTime? = null,

  @Column(name = "for_periode")
  val forPeriode: String
)
