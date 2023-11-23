package no.nav.bidrag.regnskap.service

import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.regnskap.BidragRegnskapLocal
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import java.time.LocalDateTime

@Transactional
@DirtiesContext
@ActiveProfiles("test")
@EnableMockOAuth2Server
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [BidragRegnskapLocal::class])
internal class PersistenceServiceIT {

    companion object {
        private var postgreSqlDb = PostgreSQLContainer("postgres:latest").apply {
            withDatabaseName("bidrag-regnskap")
            withUsername("cloudsqliamuser")
            withPassword("admin")
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgreSqlDb::getJdbcUrl)
            registry.add("spring.datasource.username", postgreSqlDb::getUsername)
            registry.add("spring.datasource.password", postgreSqlDb::getPassword)
        }
    }

    @Autowired
    private lateinit var persistenceService: PersistenceService

    private lateinit var oppdragTestData: no.nav.bidrag.regnskap.persistence.entity.Oppdrag

    @BeforeAll
    fun setup() {
        oppdragTestData = TestData.opprettOppdrag(oppdragsperioder = emptyList())
        val oppdragsperiode = TestData.opprettOppdragsperiode(konteringer = emptyList(), oppdrag = oppdragTestData, delytelseId = null)
        val konteringer = TestData.opprettKontering(oppdragsperiode = oppdragsperiode)
        oppdragsperiode.konteringer = listOf(konteringer)
        oppdragTestData.oppdragsperioder = listOf(oppdragsperiode)
    }

    @Test
    fun `skal lagre oppdrag`() {
        val oppdragId = persistenceService.lagreOppdrag(oppdragTestData)

        oppdragId shouldNotBe null

        val oppdrag = persistenceService.hentOppdrag(oppdragId)

        val oppdragHentetPåUnikeIdentifikatorer = persistenceService.hentOppdragPaUnikeIdentifikatorer(
            oppdragTestData.stønadType,
            oppdragTestData.kravhaverIdent,
            oppdragTestData.skyldnerIdent,
            oppdragTestData.sakId,
        )

        val oppdragHentetPåUnikeIdentifikatorerUtenTreff = persistenceService.hentOppdragPaUnikeIdentifikatorer(
            oppdragTestData.stønadType,
            "ingentreff",
            oppdragTestData.skyldnerIdent,
            oppdragTestData.sakId,
        )

        oppdrag shouldNotBe null
        oppdrag?.oppdragId shouldNotBe null
        oppdrag?.stønadType shouldBe oppdragTestData.stønadType
        oppdrag?.skyldnerIdent shouldBe oppdragTestData.skyldnerIdent
        oppdrag?.gjelderIdent shouldBe oppdragTestData.gjelderIdent
        oppdrag?.oppdragsperioder?.size shouldBe oppdragTestData.oppdragsperioder.size
        oppdrag?.oppdragsperioder?.first()?.oppdragsperiodeId shouldNotBe null
        oppdrag?.oppdragsperioder?.first()?.konteringer?.size shouldBe oppdragTestData.oppdragsperioder.first().konteringer.size
        oppdrag?.oppdragsperioder?.first()?.konteringer?.first()?.konteringId shouldNotBe null
        oppdrag?.oppdragsperioder?.first()?.konteringer?.first()?.transaksjonskode shouldBe oppdragTestData.oppdragsperioder.first().konteringer.first().transaksjonskode

        oppdragHentetPåUnikeIdentifikatorer?.oppdragId shouldNotBe null
        oppdragHentetPåUnikeIdentifikatorerUtenTreff shouldBe null
    }

    @Test
    fun `skal hente oppdrag på referanse og vedtakId`() {
        val referanse = "ReferanseSomFinnes"

        val nyttOppdrag = TestData.opprettOppdrag()
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = nyttOppdrag, referanse = referanse, vedtakId = 123)
        nyttOppdrag.oppdragsperioder = listOf(oppdragsperiode)
        val oppdragsperiodeId = persistenceService.lagreOppdragsperiode(oppdragsperiode)
        persistenceService.lagreOppdrag(nyttOppdrag)

        oppdragsperiodeId shouldNotBe null

        val oppdrag = persistenceService.hentOppdragPåReferanseOgOmgjørVedtakId(referanse, 123)

        oppdrag?.oppdragsperioder?.first()?.referanse shouldBe referanse
    }

    @Test
    fun `skal returne null ved ingen treff på referanse og vedtakId`() {
        val oppdrag = persistenceService.hentOppdragPåReferanseOgOmgjørVedtakId("ReferanseSomIkkeFinnes", 123)
        oppdrag shouldBe null
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
        val oversendtKontering = TestData.opprettKontering(overforingstidspunkt = LocalDateTime.now())
        val kontering = TestData.opprettKontering(overforingstidspunkt = null)

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

        oppdragsperiodeId1 shouldNotBe null
        oppdragsperiodeId2 shouldNotBe null
        oppdragsperiodeId3 shouldNotBe null
    }

    @Test
    fun lagreDriftsavvik() {
        val aktivtDriftsavvik =
            TestData.opprettDriftsavvik(tidspunktFra = LocalDateTime.now(), tidspunktTil = LocalDateTime.now().plusMinutes(10))
        val gammeltDriftsavvik = TestData.opprettDriftsavvik(
            tidspunktFra = LocalDateTime.now().minusMinutes(10),
            tidspunktTil = LocalDateTime.now().minusMinutes(1),
        )

        val aktivtDriftsavvikId = persistenceService.lagreDriftsavvik(aktivtDriftsavvik)
        val gammeltDriftsavvikId = persistenceService.lagreDriftsavvik(gammeltDriftsavvik)
        val harAktivtDriftsavvik = persistenceService.harAktivtDriftsavvik()
        val faktiskAktivtDriftsavvik = persistenceService.hentAlleAktiveDriftsavvik()
        val alleDriftsavvik = persistenceService.hentFlereDriftsavvik(Pageable.ofSize(100))

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
                fullførtTidspunkt = LocalDateTime.now().minusYears(100),
            ),
        )
        val driftsavvikMedPåløp = TestData.opprettDriftsavvik(
            påløpId = påløpId,
            tidspunktFra = LocalDateTime.now().minusMinutes(10),
            tidspunktTil = LocalDateTime.now().minusMinutes(1),
        )
        persistenceService.lagreDriftsavvik(driftsavvikMedPåløp)
        val driftsavvikForPåløp = persistenceService.hentDriftsavvikForPåløp(påløpId)

        driftsavvikForPåløp shouldNotBe null
    }
}
