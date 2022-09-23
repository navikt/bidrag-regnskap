package no.nav.bidrag.regnskap.persistence.entity

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
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
  val palopId: Int,

  @Column(name = "kjoredato")
  val kjoredato: LocalDate,

  @Column(name = "fullfort_tidspunkt")
  val fullfortTidspunkt: LocalDateTime?,

  @Column(name = "for_periode")
  val forPeriode: YearMonth
)
