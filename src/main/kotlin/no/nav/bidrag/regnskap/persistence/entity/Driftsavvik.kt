package no.nav.bidrag.regnskap.persistence.entity

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id


@Entity(name = "driftsavvik")
data class Driftsavvik(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "driftsavvik_id")
  val driftsavvikId: Int? = null,

  @Column(name = "palop_id")
  val påløpId: Int? = null,

  @Column(name = "tidspunkt_fra")
  val tidspunktFra: LocalDateTime,

  @Column(name = "tidspunkt_til")
  var tidspunktTil: LocalDateTime? = null,

  @Column(name = "opprettet_av")
  val opprettetAv: String? = null,

  @Column(name = "arsak")
  val årsak: String? = null
)
