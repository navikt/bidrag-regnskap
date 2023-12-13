package no.nav.bidrag.regnskap.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import java.time.LocalDateTime

@Entity(name = "konteringer")
data class Kontering(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kontering_sequence")
    @SequenceGenerator(name = "kontering_sequence", sequenceName = "kontering_id_sequence", allocationSize = 100)
    @Column(name = "kontering_id")
    val konteringId: Int = 0,

    @ManyToOne
    @JoinColumn(name = "oppdragsperiode_id")
    val oppdragsperiode: Oppdragsperiode? = null,

    @Column(name = "transaksjonskode")
    val transaksjonskode: String,

    @Column(name = "overforingsperiode")
    val overføringsperiode: String,

    @Column(name = "overforingstidspunkt")
    var overføringstidspunkt: LocalDateTime? = null,

    @Column(name = "behandlingsstatus_ok_tidspunkt")
    var behandlingsstatusOkTidspunkt: LocalDateTime? = null,

    @Column(name = "type")
    val type: String,

    @Column(name = "soknad_type")
    val søknadType: String,

    @Column(name = "siste_referansekode")
    var sisteReferansekode: String? = null,

    @Column(name = "sendt_i_palopsperiode")
    var sendtIPåløpsperiode: String? = null,

    @Column(name = "opprettet_tidspunkt")
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "vedtak_id")
    val vedtakId: Int,
) {

    override fun toString(): String {
        return this::class.simpleName +
            "(konteringId = $konteringId , " +
            "oppdragsperiodeId = ${oppdragsperiode?.oppdragsperiodeId} , " +
            "transaksjonskode = $transaksjonskode , " +
            "overføringsperiode = $overføringsperiode , " +
            "overføringstidspunkt = $overføringstidspunkt , " +
            "behandlingsstatusOkTidspunkt = $behandlingsstatusOkTidspunkt , " +
            "type = $type , " +
            "søknadType = $søknadType , " +
            "sisteReferansekode = $sisteReferansekode , " +
            "opprettetTidspunkt = $opprettetTidspunkt , " +
            "vetakId = $vedtakId , " +
            "sendtIPåløpsperiode = $sendtIPåløpsperiode )"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Kontering

        if (konteringId != other.konteringId) return false
        if (oppdragsperiode != other.oppdragsperiode) return false
        if (transaksjonskode != other.transaksjonskode) return false
        if (overføringsperiode != other.overføringsperiode) return false
        if (overføringstidspunkt != other.overføringstidspunkt) return false
        if (behandlingsstatusOkTidspunkt != other.behandlingsstatusOkTidspunkt) return false
        if (type != other.type) return false
        if (søknadType != other.søknadType) return false
        if (sisteReferansekode != other.sisteReferansekode) return false
        if (opprettetTidspunkt != other.opprettetTidspunkt) return false
        if (vedtakId != other.vedtakId) return false
        if (sendtIPåløpsperiode != other.sendtIPåløpsperiode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = konteringId
        result = 31 * result + (oppdragsperiode?.hashCode() ?: 0)
        result = 31 * result + transaksjonskode.hashCode()
        result = 31 * result + overføringsperiode.hashCode()
        result = 31 * result + (overføringstidspunkt?.hashCode() ?: 0)
        result = 31 * result + (behandlingsstatusOkTidspunkt?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        result = 31 * result + søknadType.hashCode()
        result = 31 * result + (sisteReferansekode?.hashCode() ?: 0)
        result = 31 * result + (opprettetTidspunkt.hashCode())
        result = 31 * result + (vedtakId.hashCode())
        result = 31 * result + (sendtIPåløpsperiode?.hashCode() ?: 0)
        return result
    }
}
