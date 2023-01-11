package no.nav.bidrag.regnskap.service

import io.kotest.inspectors.forNone
import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.regnskap.SpringTestRunner
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersistenceServiceIT : SpringTestRunner() {

  @Autowired
  private lateinit var persistenceService: PersistenceService

  private lateinit var oppdragTestData: no.nav.bidrag.regnskap.persistence.entity.Oppdrag

  @BeforeAll
  fun setup() {
    oppdragTestData = TestData.opprettOppdrag(oppdragsperioder = null)
    val oppdragsperiode = TestData.opprettOppdragsperiode(konteringer = null, oppdrag = oppdragTestData, delytelseId = null)
    val konteringer = TestData.opprettKontering(oppdragsperiode = oppdragsperiode, overføringKontering = null)
    oppdragsperiode.konteringer = listOf(konteringer)
    oppdragTestData.oppdragsperioder = listOf(oppdragsperiode)
  }

  @Test
  fun `skal lagre oppdrag`() {
    val oppdragId = persistenceService.lagreOppdrag(oppdragTestData)

    oppdragId shouldNotBe null

    val oppdrag = persistenceService.hentOppdrag(oppdragId!!)

    val oppdragHentetPåUnikeIdentifikatorer = persistenceService.hentOppdragPaUnikeIdentifikatorer(
      oppdragTestData.stønadType,
      oppdragTestData.kravhaverIdent,
      oppdragTestData.skyldnerIdent,
      oppdragTestData.eksternReferanse
    )

    val oppdragHentetPåUnikeIdentifikatorerUtenTreff = persistenceService.hentOppdragPaUnikeIdentifikatorer(
      oppdragTestData.stønadType,
      "ingentreff",
      oppdragTestData.skyldnerIdent,
      oppdragTestData.eksternReferanse
    )

    oppdrag shouldNotBe null
    oppdrag?.oppdragId shouldNotBe null
    oppdrag?.stønadType shouldBe oppdragTestData.stønadType
    oppdrag?.skyldnerIdent shouldBe oppdragTestData.skyldnerIdent
    oppdrag?.oppdragsperioder?.size shouldBe oppdragTestData.oppdragsperioder!!.size
    oppdrag?.oppdragsperioder?.first()?.oppdragsperiodeId shouldNotBe null
    oppdrag?.oppdragsperioder?.first()?.gjelderIdent shouldBe oppdragTestData.oppdragsperioder?.first()?.gjelderIdent
    oppdrag?.oppdragsperioder?.first()?.konteringer?.size shouldBe oppdragTestData.oppdragsperioder?.first()?.konteringer!!.size
    oppdrag?.oppdragsperioder?.first()?.konteringer?.first()?.konteringId shouldNotBe null
    oppdrag?.oppdragsperioder?.first()?.konteringer?.first()?.transaksjonskode shouldBe oppdragTestData.oppdragsperioder?.first()?.konteringer?.first()?.transaksjonskode

    oppdragHentetPåUnikeIdentifikatorer?.oppdragId shouldNotBe null
    oppdragHentetPåUnikeIdentifikatorerUtenTreff shouldBe null
  }

  @Test
  fun `skal hente oppdrag på engangsbeløpId`() {
    val engangsbeløpId = 1234
    val oppdragId =
      persistenceService.lagreOppdrag(TestData.opprettOppdrag(engangsbeløpId = engangsbeløpId, oppdragsperioder = null))

    oppdragId shouldNotBe null

    val oppdrag = persistenceService.hentOppdragPåEngangsbeløpId(engangsbeløpId)

    oppdrag?.engangsbeløpId shouldBe engangsbeløpId
  }

  @Test
  fun `skal returne null ved ingen treff på engangsbeløpId`() {
    val oppdrag = persistenceService.hentOppdragPåEngangsbeløpId(12346)
    oppdrag shouldBe null
  }

  @Test
  fun `skal opprette overføringKontering`() {
    val overføringKonteringId = persistenceService.lagreOverføringKontering(TestData.opprettOverføringKontering())
    val overføringKonteringMedFeilId =
      persistenceService.lagreOverføringKontering(TestData.opprettOverføringKontering(feilmelding = "TestFeil"))
    val overføringKonteringerListe = persistenceService.hentOverføringKontering(Pageable.ofSize(10))
    val overføringKonteringerMedFeilListe = persistenceService.hentOverføringKonteringMedFeil(Pageable.ofSize(10))

    overføringKonteringId shouldNotBe null
    overføringKonteringMedFeilId shouldNotBe null
    overføringKonteringerListe.size shouldBeGreaterThanOrEqual 2
    overføringKonteringerMedFeilListe.size shouldBeGreaterThanOrEqual 1
  }

  @Test
  fun `skal lagre nytt påløp`() {
    val påløpJan = TestData.opprettPåløp(forPeriode = "2022-01")
    val påløpFeb = TestData.opprettPåløp(forPeriode = "2022-02")

    val påløpJanId = persistenceService.lagrePåløp(påløpJan)
    val påløpFebId = persistenceService.lagrePåløp(påløpFeb)

    påløpJanId shouldNotBe null
    påløpFebId shouldNotBe null

    val påløpListe = persistenceService.hentPåløp()

    påløpListe.map { it.forPeriode }.toList() shouldContainAll listOf(påløpJan.forPeriode, påløpFeb.forPeriode)
  }

  @Test
  fun lagreKontering() {
    val oversendtKontering = TestData.opprettKontering(overføringKontering = null, overforingstidspunkt = LocalDateTime.now())
    val kontering = TestData.opprettKontering(overføringKontering = null, overforingstidspunkt = null)

    val konteringId = persistenceService.lagreKontering(kontering)
    val oversendtKonteringId = persistenceService.lagreKontering(oversendtKontering)
    val ikkeOverførteKonteringer = persistenceService.hentAlleIkkeOverførteKonteringer()
    val konteringerForDato = persistenceService.hentAlleKonteringerForDato(LocalDate.now())

    konteringId shouldNotBe null
    oversendtKonteringId shouldNotBe null
    konteringerForDato.forOne { it.konteringId shouldBe oversendtKonteringId }
    ikkeOverførteKonteringer.forOne { it.konteringId shouldBe konteringId }
  }


    @Test
    fun lagreOppdragsperiode() {
      val oppdragsperiode1 = TestData.opprettOppdragsperiode(vedtakId = 100, aktivTil = null)
      val oppdragsperiode2 = TestData.opprettOppdragsperiode(vedtakId = 101, aktivTil = LocalDate.now().plusDays(1))
      val oppdragsperiode3 = TestData.opprettOppdragsperiode(vedtakId = 102, aktivTil = LocalDate.now().minusDays(1))

      val oppdragsperiodeId1 = persistenceService.lagreOppdragsperiode(oppdragsperiode1)
      val oppdragsperiodeId2 = persistenceService.lagreOppdragsperiode(oppdragsperiode2)
      val oppdragsperiodeId3 = persistenceService.lagreOppdragsperiode(oppdragsperiode3)

      val oppdragsperioder = persistenceService.hentAlleOppdragsperioderSomErAktiveForPeriode(LocalDate.now())

      oppdragsperiodeId1 shouldNotBe null
      oppdragsperiodeId2 shouldNotBe null
      oppdragsperiodeId3 shouldNotBe null
      oppdragsperioder.forOne { it.vedtakId shouldBe 100 }
      oppdragsperioder.forOne { it.vedtakId shouldBe 101 }
      oppdragsperioder.forNone { it.vedtakId shouldBe 102 }
  }
    @Test
    fun lagreDriftsavvik() {
      val aktivtDriftsavvik =
        TestData.opprettDriftsavvik(tidspunktFra = LocalDateTime.now(), tidspunktTil = LocalDateTime.now().plusMinutes(10))
      val gammeltDriftsavvik = TestData.opprettDriftsavvik(
        tidspunktFra = LocalDateTime.now().minusMinutes(10),
        tidspunktTil = LocalDateTime.now().minusMinutes(1)
      )

      val aktivtDriftsavvikId = persistenceService.lagreDriftsavvik(aktivtDriftsavvik)
      val gammeltDriftsavvikId = persistenceService.lagreDriftsavvik(gammeltDriftsavvik)
      val harAktivtDriftsavvik = persistenceService.harAktivtDriftsavvik()
      val faktiskAktivtDriftsavvik = persistenceService.hentAlleAktiveDriftsavvik()
      val alleDriftsavvik = persistenceService.hentDriftsavvik(Pageable.ofSize(100))

      aktivtDriftsavvikId shouldNotBe null
      gammeltDriftsavvikId shouldNotBe null
      harAktivtDriftsavvik shouldBe true
      faktiskAktivtDriftsavvik.forOne { it.driftsavvikId shouldBe aktivtDriftsavvikId }
      alleDriftsavvik.map { it.driftsavvikId }.toList() shouldContainAll listOf(aktivtDriftsavvikId, gammeltDriftsavvikId)
    }

    @Test
    fun hentDriftsavvikForPåløp() {
      val påløpId = persistenceService.lagrePåløp(
        TestData.opprettPåløp(
          forPeriode = "1900-01",
          fullførtTidspunkt = LocalDateTime.now().minusYears(100)
        )
      )!!
      val driftsavvikMedPåløp = TestData.opprettDriftsavvik(
        påløpId = påløpId,
        tidspunktFra = LocalDateTime.now().minusMinutes(10),
        tidspunktTil = LocalDateTime.now().minusMinutes(1)
      )
      persistenceService.lagreDriftsavvik(driftsavvikMedPåløp)
      val driftsavvikForPåløp = persistenceService.hentDriftsavvikForPåløp(påløpId)

      driftsavvikForPåløp shouldNotBe null
    }
}