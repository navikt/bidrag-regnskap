package no.nav.bidrag.regnskap.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity(name = "palop")
data class Påløp(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "palop_id")
    val påløpId: Int = 0,

    @Column(name = "kjoredato")
    val kjøredato: LocalDateTime,

    @Column(name = "startet_tidspunkt")
    val startetTidspunkt: LocalDateTime? = null,

    @Column(name = "fullfort_tidspunkt")
    val fullførtTidspunkt: LocalDateTime? = null,

    @Column(name = "for_periode")
    val forPeriode: String,
) {

    override fun toString(): String {
        return this::class.simpleName +
            "(påløpId = $påløpId , kjøredato = $kjøredato , fullførtTidspunkt = $fullførtTidspunkt , forPeriode = $forPeriode )"
    }
}
