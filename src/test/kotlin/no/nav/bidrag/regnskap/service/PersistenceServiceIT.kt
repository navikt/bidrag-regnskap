package no.nav.bidrag.regnskap.service

import io.kotest.inspectors.forNone
import io.kotest.inspectors.forOne
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.regnskap.BidragRegnskapLocal
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth


@Testcontainers
@EnableMockOAuth2Server
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [BidragRegnskapLocal::class])
internal class PersistenceServiceIT {

  companion object {
    @Container
    val postgreSQLContainer = PostgreSQLContainer("postgres:latest").apply {
      withDatabaseName("bidrag-regnskap")
      withUsername("cloudsqliamuser")
      withPassword("admin")
      start()
    }

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl)
      registry.add("spring.datasource.username", postgreSQLContainer::getUsername)
      registry.add("spring.datasource.password", postgreSQLContainer::getPassword)
    }
  }

  @Autowired
  private lateinit var persistenceService: PersistenceService

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
  open inner class Oppdrag {

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
    @Order(1)
    fun `skal lagre oppdrag`() {
      val oppdragId = persistenceService.lagreOppdrag(oppdragTestData)

      oppdragId shouldBe 1
    }

    @Test
    @Order(2)
    @Transactional
    open fun `skal hente opp lagret oppdrag`() {
      val oppdrag = persistenceService.hentOppdrag(1)

      oppdrag shouldNotBe null
      oppdrag.get().oppdragId shouldNotBe null
      oppdrag.get().stønadType shouldBe oppdragTestData.stønadType
      oppdrag.get().skyldnerIdent shouldBe oppdragTestData.skyldnerIdent
      oppdrag.get().oppdragsperioder!!.size shouldBe oppdragTestData.oppdragsperioder!!.size
      oppdrag.get().oppdragsperioder!!.first().oppdragsperiodeId shouldNotBe null
      oppdrag.get().oppdragsperioder!!.first().delytelseId!! shouldBeGreaterThan 100000
      oppdrag.get().oppdragsperioder!!.first().gjelderIdent shouldBe oppdragTestData.oppdragsperioder!!.first().gjelderIdent
      oppdrag.get().oppdragsperioder!!.first().konteringer!!.size shouldBe oppdragTestData.oppdragsperioder!!.first().konteringer!!.size
      oppdrag.get().oppdragsperioder!!.first().konteringer!!.first().konteringId shouldNotBe null
      oppdrag.get().oppdragsperioder!!.first().konteringer!!.first().transaksjonskode shouldBe oppdragTestData.oppdragsperioder!!.first().konteringer!!.first().transaksjonskode
    }

    @Test
    @Order(2)
    fun `skal hente oppdrag på unike identifikatorer om oppdrag finnes`() {
      val oppdrag = persistenceService.hentOppdragPaUnikeIdentifikatorer(
        oppdragTestData.stønadType,
        oppdragTestData.kravhaverIdent,
        oppdragTestData.skyldnerIdent,
        oppdragTestData.eksternReferanse
      )

      oppdrag.get().oppdragId shouldNotBe null
    }

    @Test
    @Order(2)
    fun `skal returne tomt ved ingen treff på unike identifikatorer`() {
      val oppdrag = persistenceService.hentOppdragPaUnikeIdentifikatorer(
        oppdragTestData.stønadType,
        "ingentreff",
        oppdragTestData.skyldnerIdent,
        oppdragTestData.eksternReferanse
      )

      oppdrag.isEmpty shouldBe true
    }

    @Test
    @Order(2)
    fun `skal hente oppdrag på engangsbeløpId`() {
      val engangsbeløpId = 1234

      persistenceService.lagreOppdrag(TestData.opprettOppdrag(engangsbeløpId = engangsbeløpId, oppdragsperioder = null))

      val oppdrag = persistenceService.hentOppdragPåEngangsbeløpId(engangsbeløpId)

      oppdrag.get().engangsbeløpId shouldBe engangsbeløpId
    }

    @Test
    @Order(2)
    fun `skal returne tomt ved ingen treff på engangsbeløpId`() {
      val engangsbeløpId = 12346

      val oppdrag = persistenceService.hentOppdragPåEngangsbeløpId(engangsbeløpId)

      oppdrag.isEmpty shouldBe true
    }
  }

  @Nested
  @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
  inner class OverføringKontering {

    @Test
    @Order(1)
    fun `skal opprette overføringKontering kall uten feil`() {
      val overføringKontering = TestData.opprettOverføringKontering()
      val overføringId = persistenceService.lagreOverføringKontering(overføringKontering)
      overføringId shouldNotBe null
    }

    @Test
    @Order(2)
    fun `skal opprette overføringKontering med feil`() {
      val overføringKontering = TestData.opprettOverføringKontering(feilmelding = "TestFeil")
      val overføringId = persistenceService.lagreOverføringKontering(overføringKontering)
      overføringId shouldNotBe null
    }

    @Test
    @Order(3)
    fun `skal hente overføring konteringer`() {
      val overføringKonteringer = persistenceService.hentOverføringKontering(Pageable.ofSize(10))

      overføringKonteringer.size shouldBe 2
    }

    @Test
    @Order(3)
    fun `skal hente overføring med feil konteringer`() {
      val overføringKonteringer = persistenceService.hentOverføringKonteringMedFeil(Pageable.ofSize(10))

      overføringKonteringer.size shouldBe 1
    }
  }

  @Nested
  @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
  inner class Påløp {

    @Test
    @Order(1)
    fun `skal lagre nytt påløp`() {
      val påløpJan = TestData.opprettPåløp(forPeriode = "2022-01")
      val påløpFeb = TestData.opprettPåløp(forPeriode = "2022-02")

      val påløpJanId = persistenceService.lagrePåløp(påløpJan)
      val påløpFebId = persistenceService.lagrePåløp(påløpFeb)

      påløpJanId shouldNotBe null
      påløpFebId shouldNotBe null
    }

    @Test
    @Order(2)
    fun `skal hente lagrede påløp`() {
      val påløp = persistenceService.hentPåløp()

      påløp.size shouldBe 2
    }

    @Test
    @Order(3)
    fun `skal hente påløp i januar, fullføre det og deretter hente februar`() {
      val påløpJan = persistenceService.hentIkkeKjørtePåløp().minByOrNull { it.forPeriode }!!
      påløpJan.fullførtTidspunkt = LocalDateTime.now()
      persistenceService.lagrePåløp(påløpJan)

      val påløpFeb = persistenceService.hentIkkeKjørtePåløp()

      påløpFeb.first().forPeriode shouldBe "2022-02"
    }

    @Test
    @Order(4)
    fun `skal hente siste overførte periode som burde være januar`() {
      val påløpJan = persistenceService.finnSisteOverførtePeriode()

      påløpJan shouldBe YearMonth.parse("2022-01")
    }
  }

  @Nested
  @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
  inner class Kontering {

    @Test
    @Order(1)
    fun lagreKontering() {
      val oversendtKontering = TestData.opprettKontering(overføringKontering = null, overforingstidspunkt = LocalDateTime.now())
      val kontering = TestData.opprettKontering(overføringKontering = null, overforingstidspunkt = null)

      val konteringId = persistenceService.lagreKontering(kontering)
      val oversendtKonteringId = persistenceService.lagreKontering(oversendtKontering)

      konteringId shouldNotBe null
      oversendtKonteringId shouldNotBe null
    }

    @Test
    @Order(2)
    fun hentAlleIkkeOverførteKonteringer() {
      val konteringer = persistenceService.hentAlleIkkeOverførteKonteringer()

      konteringer.size shouldNotBe 0
    }

    @Test
    @Order(3)
    fun hentAlleKonteringerForDato() {
      val konteringerForDato = persistenceService.hentAlleKonteringerForDato(LocalDate.now())

      konteringerForDato.size shouldBe 1
    }
  }

  @Nested
  @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
  inner class Oppdragsperiode {

    @Test
    @Order(1)
    fun lagreOppdragsperiode() {
      val oppdragsperiode1 = TestData.opprettOppdragsperiode(vedtakId = 100, aktivTil = null)
      val oppdragsperiode2 = TestData.opprettOppdragsperiode(vedtakId = 101, aktivTil = LocalDate.now().plusDays(1))
      val oppdragsperiode3 = TestData.opprettOppdragsperiode(vedtakId = 102, aktivTil = LocalDate.now().minusDays(1))

      val oppdragsperiodeId1 = persistenceService.lagreOppdragsperiode(oppdragsperiode1)
      val oppdragsperiodeId2 = persistenceService.lagreOppdragsperiode(oppdragsperiode2)
      val oppdragsperiodeId3 = persistenceService.lagreOppdragsperiode(oppdragsperiode3)

      oppdragsperiodeId1 shouldNotBe null
      oppdragsperiodeId2 shouldNotBe null
      oppdragsperiodeId3 shouldNotBe null
    }

    @Test
    @Order(2)
    fun hentAlleOppdragsperioderSomErAktiveForPeriode() {
      val oppdragsperioder = persistenceService.hentAlleOppdragsperioderSomErAktiveForPeriode(LocalDate.now())

      oppdragsperioder.forOne { oppdragsperiode ->
        oppdragsperiode.vedtakId shouldBe 100
      }
      oppdragsperioder.forOne { oppdragsperiode ->
        oppdragsperiode.vedtakId shouldBe 101
      }
      oppdragsperioder.forNone { oppdragsperiode ->
        oppdragsperiode.vedtakId shouldBe 102
      }
    }
  }

  @Nested
  @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
  inner class Driftsavvik {

    @Test
    @Order(1)
    fun lagreDriftsavvik() {
      val aktivtDriftsavvik = TestData.opprettDriftsavvik(tidspunktFra = LocalDateTime.now(), tidspunktTil = LocalDateTime.now().plusMinutes(10))
      val gammeltDriftsavvik = TestData.opprettDriftsavvik(tidspunktFra = LocalDateTime.now().minusMinutes(10), tidspunktTil = LocalDateTime.now().minusMinutes(1))

      val aktivtDriftsavvikId = persistenceService.lagreDriftsavvik(aktivtDriftsavvik)
      val gammeltDriftsavvikId = persistenceService.lagreDriftsavvik(gammeltDriftsavvik)

      aktivtDriftsavvikId shouldNotBe null
      gammeltDriftsavvikId shouldNotBe null
    }

    @Test
    @Order(2)
    fun finnesAktivtDriftsavvik() {
      val aktivtDriftsavvik = persistenceService.finnesAktivtDriftsavvik()

      aktivtDriftsavvik shouldBe true
    }

    @Test
    @Order(2)
    fun hentAlleAktiveDriftsavvik() {
      val aktivtDriftsavvik = persistenceService.hentAlleAktiveDriftsavvik()

      aktivtDriftsavvik.size shouldBe 1
    }

    @Test
    @Order(2)
    fun hentDriftsavvik() {
      val driftsavvik = persistenceService.hentDriftsavvik(Pageable.ofSize(10))

      driftsavvik.size shouldBe 2
    }

    @Test
    @Order(3)
    fun hentDriftsavvikForPåløp() {
      val påløpId = persistenceService.lagrePåløp(TestData.opprettPåløp(forPeriode = "1900-01", fullførtTidspunkt = LocalDateTime.now().minusYears(100)))!!
      val driftsavvikMedPåløp = TestData.opprettDriftsavvik(påløpId = påløpId, tidspunktFra = LocalDateTime.now().minusMinutes(10), tidspunktTil = LocalDateTime.now().minusMinutes(1))
      persistenceService.lagreDriftsavvik(driftsavvikMedPåløp)
      val driftsavvikForPåløp = persistenceService.hentDriftsavvikForPåløp(påløpId)

      driftsavvikForPåløp shouldNotBe null
    }
  }
}