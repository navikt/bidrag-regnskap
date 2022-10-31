package no.nav.bidrag.regnskap.persistence.entity

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id


@Entity(name = "drift_avvik")
data class DriftAvvik(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "avvik_id")
  val avvikId: Int,

  @Column(name = "palop_id")
  val påløpId: Int?,

  @Column(name = "tidspunkt_fra")
  val tidspunktFra: LocalDateTime,

  @Column(name = "tidspunkt_til")
  val tidspunktTil: LocalDateTime?,

  @Column(name = "opprettet_av")
  val opprettetAv: String?,

  @Column(name = "arsak")
  val årsak: String?
)
