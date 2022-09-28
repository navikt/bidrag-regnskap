package no.nav.bidrag.regnskap.persistence.entity

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity(name = "overforing_konteringer")
data class OverforingKontering(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "overforing_id")
  val overforingId: Int,

  @Column(name = "kontering_id")
  val konteringId: Int,

  @Column(name = "referansekode")
  val referansekode: String,

  @Column(name = "feilmelding")
  val feilmelding: String,

  @Column(name = "tidspunkt")
  val tidspunkt: LocalDateTime,

  @Column(name = "kanal")
  val kanal: String?
) {
  override fun toString(): String {
    return ""
  }
}
