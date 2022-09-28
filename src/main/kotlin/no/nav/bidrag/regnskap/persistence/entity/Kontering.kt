package no.nav.bidrag.regnskap.persistence.entity

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity(name = "konteringer")
data class Kontering(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "kontering_id")
  val konteringId: Int? = null,

  @ManyToOne
  @JoinColumn(name = "oppdragsperiode_id")
  val oppdragsperiode: Oppdragsperiode? = null,

  @Column(name = "transaksjonskode")
  val transaksjonskode: String,

  @Column(name = "overforingsperiode")
  val overforingsperiode: String,

  @Column(name = "overforinstidspunkt")
  val overforingstidspunkt: LocalDateTime? = null,

  @Column(name = "type")
  val type: String?,

  @Column(name = "justering")
  val justering: String?,

  @Column(name = "gebyr_rolle")
  val gebyrRolle: String?,

  @Column(name = "sendt_i_palopsfil")
  val sendtIPalopsfil: Boolean = false
)

