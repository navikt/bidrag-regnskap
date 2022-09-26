package no.nav.bidrag.regnskap.persistence.entity

import java.time.LocalDate
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany

@Entity(name = "oppdrag")
data class Oppdrag(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "oppdrag_id")
  var oppdragId: Int? = null,

  @Column(name = "stonad_type")
  val stonadType: String,

  @Column(name = "kravhaver_ident")
  val kravhaverIdent: String?,

  @Column(name = "skyldner_ident")
  val skyldnerIdent: String,

  @Column(name = "sak_id")
  val sakId: Int,

  @Column(name = "referanse")
  val referanse: String?,

  @Column(name = "utsatt_til_dato")
  val utsattTilDato: LocalDate?,

  @OneToMany(mappedBy = "oppdragId", cascade = [CascadeType.ALL])
  val oppdragsperioder: List<Oppdragsperiode>? = null
)
