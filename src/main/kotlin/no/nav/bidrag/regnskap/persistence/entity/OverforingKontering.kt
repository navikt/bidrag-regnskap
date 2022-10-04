package no.nav.bidrag.regnskap.persistence.entity

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity(name = "overforing_konteringer")
data class OverforingKontering(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "overforing_id")
  val overforingId: Int? = null,

  @ManyToOne
  @JoinColumn(name = "kontering_id")
  val kontering: Kontering? = null,

  @Column(name = "referansekode")
  val referansekode: String? = null,

  @Column(name = "feilmelding")
  val feilmelding: String? = null,

  @Column(name = "tidspunkt")
  val tidspunkt: LocalDateTime,

  @Column(name = "kanal")
  val kanal: String
) {
  override fun toString(): String {
    return ""
  }
}
