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
data class OverføringKontering(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "overforing_id")
  val overføringId: Int = 0,

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
    return this::class.simpleName +
        "(overføringId = $overføringId , " +
        "konteringId = ${kontering?.konteringId} , " +
        "referansekode = $referansekode , " +
        "feilmelding = $feilmelding , " +
        "tidspunkt = $tidspunkt , " +
        "kanal = $kanal )"
  }
}
