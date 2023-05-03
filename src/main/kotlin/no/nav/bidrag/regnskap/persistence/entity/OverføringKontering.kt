package no.nav.bidrag.regnskap.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

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
