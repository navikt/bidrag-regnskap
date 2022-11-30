package no.nav.bidrag.regnskap.persistence.entity

import org.hibernate.Hibernate
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
  val overføringId: Int? = null,

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
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OverføringKontering

    return overføringId != null && overføringId == other.overføringId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
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
