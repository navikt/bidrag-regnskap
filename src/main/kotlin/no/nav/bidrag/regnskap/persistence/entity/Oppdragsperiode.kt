package no.nav.bidrag.regnskap.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.SequenceGenerator
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.DynamicInsert
import java.math.BigDecimal
import java.time.LocalDate

@Entity(name = "oppdragsperioder")
@DynamicInsert
data class Oppdragsperiode(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "oppdragsperiode_sequence")
    @SequenceGenerator(name = "oppdragsperiode_sequence", sequenceName = "oppdragsperiode_id_sequence", allocationSize = 100)
    @Column(name = "oppdragsperiode_id")
    val oppdragsperiodeId: Int = 0,

    @ManyToOne
    @JoinColumn(name = "oppdrag_id")
    val oppdrag: Oppdrag? = null,

    @Column(name = "vedtak_id")
    val vedtakId: Int,

    @Column(name = "referanse")
    val referanse: String?,

    @Column(name = "vedtak_type")
    var vedtakType: String,

    @Column(name = "belop")
    val beløp: BigDecimal,

    @Column(name = "valuta")
    val valuta: String,

    @Column(name = "periode_fra")
    val periodeFra: LocalDate,

    @Column(name = "periode_til")
    val periodeTil: LocalDate?,

    @Column(name = "vedtaksdato")
    val vedtaksdato: LocalDate,

    @Column(name = "aktiv_til")
    var aktivTil: LocalDate? = null,

    @Column(name = "opphorende_oppdragsperiode")
    var opphørendeOppdragsperiode: Boolean = false,

    @Column(name = "opprettet_av")
    val opprettetAv: String,

    @Column(name = "enhetsnummer")
    val enhetsnummer: String? = null,

    @Column(name = "konteringer_fullfort_opprettet")
    var konteringerFullførtOpprettet: Boolean = false,

    @Column(name = "delytelses_id")
    @ColumnDefault("nextval('delytelsesId_seq')")
    val delytelseId: Int?,

    @Column(name = "ekstern_referanse")
    val eksternReferanse: String? = null,

    @OneToMany(mappedBy = "oppdragsperiode", cascade = [CascadeType.ALL])
    @OrderBy("konteringId")
    var konteringer: List<Kontering> = emptyList(),
) {

    override fun toString(): String {
        return this::class.simpleName +
            "(oppdragsperiodeId = $oppdragsperiodeId , " +
            "oppdragId = ${oppdrag?.oppdragId} , " +
            "vedtakId = $vedtakId , " +
            "referanse = $referanse , " +
            "vedtakType = $vedtakType , " +
            "beløp = $beløp , " +
            "valuta = $valuta , " +
            "periodeFra = $periodeFra , " +
            "periodeTil = $periodeTil , " +
            "vedtaksdato = $vedtaksdato , " +
            "opprettetAv = $opprettetAv , " +
            "konteringerFullførtOpprettet = $konteringerFullførtOpprettet , " +
            "delytelseId = $delytelseId , " +
            "eksternReferanse = $eksternReferanse , " +
            "opphørendeOppdragsperiode = $opphørendeOppdragsperiode , " +
            "aktivTil = $aktivTil )"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Oppdragsperiode

        if (oppdragsperiodeId != other.oppdragsperiodeId) return false
        if (oppdrag != other.oppdrag) return false
        if (vedtakId != other.vedtakId) return false
        if (referanse != other.referanse) return false
        if (vedtakType != other.vedtakType) return false
        if (beløp != other.beløp) return false
        if (valuta != other.valuta) return false
        if (periodeFra != other.periodeFra) return false
        if (periodeTil != other.periodeTil) return false
        if (vedtaksdato != other.vedtaksdato) return false
        if (opprettetAv != other.opprettetAv) return false
        if (konteringerFullførtOpprettet != other.konteringerFullførtOpprettet) return false
        if (delytelseId != other.delytelseId) return false
        if (eksternReferanse != other.eksternReferanse) return false
        if (opphørendeOppdragsperiode != other.opphørendeOppdragsperiode) return false
        if (aktivTil != other.aktivTil) return false

        return true
    }

    override fun hashCode(): Int {
        var result = oppdragsperiodeId
        result = 31 * result + (oppdrag?.hashCode() ?: 0)
        result = 31 * result + vedtakId
        result = 31 * result + (referanse?.hashCode() ?: 0)
        result = 31 * result + vedtakType.hashCode()
        result = 31 * result + beløp.hashCode()
        result = 31 * result + valuta.hashCode()
        result = 31 * result + periodeFra.hashCode()
        result = 31 * result + (periodeTil?.hashCode() ?: 0)
        result = 31 * result + vedtaksdato.hashCode()
        result = 31 * result + opprettetAv.hashCode()
        result = 31 * result + konteringerFullførtOpprettet.hashCode()
        result = 31 * result + (delytelseId ?: 0)
        result = 31 * result + (eksternReferanse?.hashCode() ?: 0)
        result = 31 * result + (opphørendeOppdragsperiode.hashCode())
        result = 31 * result + (aktivTil?.hashCode() ?: 0)
        return result
    }
}
