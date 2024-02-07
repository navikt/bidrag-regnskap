package no.nav.bidrag.regnskap.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Version
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(name = "oppdrag")
data class Oppdrag(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "oppdrag_sequence")
    @SequenceGenerator(name = "oppdrag_sequence", sequenceName = "oppdrag_id_sequence", allocationSize = 100)
    @Column(name = "oppdrag_id")
    val oppdragId: Int = 0,

    @Column(name = "stonad_type")
    val stønadType: String,

    @Column(name = "sak_id")
    val sakId: String,

    @Column(name = "kravhaver_ident")
    var kravhaverIdent: String? = null,

    @Column(name = "skyldner_ident")
    var skyldnerIdent: String,

    @Column(name = "gjelder_ident")
    var gjelderIdent: String,

    @Column(name = "mottaker_ident")
    var mottakerIdent: String,

    @Column(name = "utsatt_til_dato")
    var utsattTilDato: LocalDate? = null,

    @Column(name = "endret_tidspunkt")
    @Version
    var endretTidspunkt: LocalDateTime? = null,

    @OneToMany(mappedBy = "oppdrag", cascade = [CascadeType.ALL])
    @OrderBy("oppdragsperiodeId")
    var oppdragsperioder: List<Oppdragsperiode> = emptyList(),
) {

    override fun toString(): String {
        return this::class.simpleName +
            "(oppdragId = $oppdragId , " +
            "stønadType = $stønadType , " +
            "sakId = $sakId , " +
            "kravhaverIdent = $kravhaverIdent , " +
            "skyldnerIdent = $skyldnerIdent , " +
            "mottakerIdent = $mottakerIdent , " +
            "gjelderIdent = $gjelderIdent , " +
            "utsattTilDato = $utsattTilDato , " +
            "endretTidspunkt = $endretTidspunkt )"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Oppdrag

        if (oppdragId != other.oppdragId) return false
        if (stønadType != other.stønadType) return false
        if (sakId != other.sakId) return false
        if (kravhaverIdent != other.kravhaverIdent) return false
        if (skyldnerIdent != other.skyldnerIdent) return false
        if (mottakerIdent != other.mottakerIdent) return false
        if (gjelderIdent != other.gjelderIdent) return false
        if (utsattTilDato != other.utsattTilDato) return false
        if (endretTidspunkt != other.endretTidspunkt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = oppdragId
        result = 31 * result + stønadType.hashCode()
        result = 31 * result + sakId.hashCode()
        result = 31 * result + (kravhaverIdent?.hashCode() ?: 0)
        result = 31 * result + skyldnerIdent.hashCode()
        result = 31 * result + mottakerIdent.hashCode()
        result = 31 * result + gjelderIdent.hashCode()
        result = 31 * result + (utsattTilDato?.hashCode() ?: 0)
        result = 31 * result + (endretTidspunkt?.hashCode() ?: 0)
        return result
    }
}
