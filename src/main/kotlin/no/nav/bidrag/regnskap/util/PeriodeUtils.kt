package no.nav.bidrag.regnskap.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors
import java.util.stream.Stream

object PeriodeUtils {

    /**
     * Metode som finner alle YearMonths, omtalt som perioder, mellom to datoer. Om datoTil ikke er satt tas det utgangspunk
     * i siste overførte periode. Denne vil i de fleste tilfeller være siste kjørte påløpsdato.
     */
    fun hentAllePerioderMellomDato(
        periodeFraForOppdragsperiode: LocalDate,
        periodeTilForOppdragsperiode: LocalDate?,
        sisteOverførtePeriode: YearMonth
    ): List<YearMonth> {
        val periodeTil =
            periodeTilForOppdragsperiode ?: LocalDate.of(sisteOverførtePeriode.year, sisteOverførtePeriode.month, 1).plusMonths(1)

        if (periodeTil.isBefore(periodeFraForOppdragsperiode)) {
            return emptyList()
        }

        // Finner alle perioder som er mellom fra og med periodeFra og til og med periodeTil
        // (Om den eksisterer, ellers brukes siste overførte periode)
        return Stream.iterate(periodeFraForOppdragsperiode) { date: LocalDate -> date.plusMonths(1) }.limit(
            ChronoUnit.MONTHS.between(
                periodeFraForOppdragsperiode,
                periodeTil
            )
        ).map { it.format(DateTimeFormatter.ofPattern("yyyy-MM")) }.map { YearMonth.parse(it) }.collect(Collectors.toList())
    }

    fun erFørsteDatoSammeSomEllerTidligereEnnAndreDato(
        førsteDato: LocalDate?,
        andreDato: LocalDate
    ): Boolean {
        return førsteDato != null && (førsteDato.isBefore(andreDato) || førsteDato.isEqual(andreDato))
    }
}
