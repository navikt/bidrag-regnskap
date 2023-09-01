package no.nav.bidrag.regnskap.hendelse.vedtak

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import no.nav.bidrag.behandling.felles.dto.vedtak.VedtakHendelse
import no.nav.bidrag.behandling.felles.enums.EngangsbelopType
import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.behandling.felles.enums.StonadType.BIDRAG
import no.nav.bidrag.behandling.felles.enums.StonadType.BIDRAG18AAR
import no.nav.bidrag.behandling.felles.enums.StonadType.EKTEFELLEBIDRAG
import no.nav.bidrag.behandling.felles.enums.StonadType.FORSKUDD
import no.nav.bidrag.behandling.felles.enums.StonadType.MOTREGNING
import no.nav.bidrag.behandling.felles.enums.StonadType.OPPFOSTRINGSBIDRAG
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.regnskap.BidragRegnskapLocal
import no.nav.bidrag.regnskap.consumer.KravApiWireMock
import no.nav.bidrag.regnskap.consumer.SakApiWireMock
import no.nav.bidrag.regnskap.dto.enumer.Søknadstype
import no.nav.bidrag.regnskap.dto.enumer.Søknadstype.EN
import no.nav.bidrag.regnskap.dto.enumer.Søknadstype.FABM
import no.nav.bidrag.regnskap.dto.enumer.Søknadstype.FABP
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.A1
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.A3
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.B1
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.B3
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.D1
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.D3
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.E1
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.E3
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.F1
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.F3
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.G1
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.G3
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.H1
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.H3
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.I1
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.K1
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.K2
import no.nav.bidrag.regnskap.dto.enumer.Transaksjonskode.K3
import no.nav.bidrag.regnskap.dto.enumer.Type.ENDRING
import no.nav.bidrag.regnskap.dto.enumer.Type.NY
import no.nav.bidrag.regnskap.maskinporten.MaskinportenWireMock
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.service.KravService
import no.nav.bidrag.regnskap.service.PersistenceService
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import org.testcontainers.shaded.org.awaitility.Durations.TEN_SECONDS
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Transactional
@DirtiesContext
@ActiveProfiles("test")
@EnableMockOAuth2Server
@TestInstance(PER_CLASS)
@TestMethodOrder(OrderAnnotation::class)
@SpringBootTest(classes = [BidragRegnskapLocal::class])
@EmbeddedKafka(partitions = 1, brokerProperties = ["listeners=PLAINTEXT://localhost:9092", "port=9092"])
internal class VedtakshendelseListenerIT {

    companion object {
        private const val HENDELSE_FILMAPPE = "testfiler/hendelse/"
        private const val TESTDATA_OUTPUT_NAVN = "kravTestData.json"
        private val PÅLØPSDATO = LocalDate.of(2022, 6, 1)

        private var kravApiWireMock: KravApiWireMock = KravApiWireMock()
        private var sakApiWireMock: SakApiWireMock = SakApiWireMock()
        private var maskinportenWireMock: MaskinportenWireMock = MaskinportenWireMock()

        private val maskinportenConfig = MaskinportenWireMock.createMaskinportenConfig()

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
            registry.add("maskinporten.privateKey", maskinportenConfig::privateKey)
            registry.add("maskinporten.tokenUrl", maskinportenConfig::tokenUrl)
            registry.add("maskinporten.audience", maskinportenConfig::audience)
            registry.add("maskinporten.clientId", maskinportenConfig::clientId)
            registry.add("maskinporten.scope", maskinportenConfig::scope)
        }
    }

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    private lateinit var persistenceService: PersistenceService

    @Autowired
    private lateinit var kravService: KravService

    @Value("\${TOPIC_VEDTAK}")
    private lateinit var topic: String

    private lateinit var file: FileOutputStream

    private val påløp =
        TestData.opprettPåløp(forPeriode = YearMonth.from(PÅLØPSDATO).toString(), fullførtTidspunkt = LocalDateTime.now())

    private val objectmapper = jacksonObjectMapper().registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

    @BeforeAll
    fun beforeAll() {
        persistenceService.konteringRepository.deleteAll()
        persistenceService.oppdragsperiodeRepository.deleteAll()
        persistenceService.oppdragRepository.deleteAll()
        file = FileOutputStream(TESTDATA_OUTPUT_NAVN)
        persistenceService.lagrePåløp(påløp)
    }

    @BeforeEach
    fun beforeEach() {
        kravApiWireMock.kravMedGyldigResponse()
        kravApiWireMock.behandlingsstatusMedGyldigResponse()
        sakApiWireMock.sakMedGyldigResponse()
        kravApiWireMock.livenessMedGyldigResponse()
        maskinportenWireMock.kravMedGyldigResponse()
    }

    @AfterAll
    internal fun teardown() {
        file.close()
    }

    @Test
    @Order(1)
    fun `skal opprette gybyr for skyldner`() {
        val vedtakHendelse = hentFilOgSendPåKafka("gebyrSkyldner.json", 1)

        val kontering = assertVedOpprettelseAvEngangsbeløp(
            1,
            vedtakHendelse,
            EngangsbelopType.GEBYR_SKYLDNER,
            G1,
            Integer.valueOf(vedtakHendelse.engangsbelopListe!![0].delytelseId),
            FABP
        )

        skrivTilTestdatafil(listOf(kontering), "Gebyr for skyldner")
    }

    @Test
    @Order(2)
    fun `skal oppdatere gebyr for skyldner`() {
        hentFilOgSendPåKafka("gebyrSkyldnerOppdatering.json", 3)

        val konteringer = assertVedOppdateringAvEngangsbeløpOgReturnerKonteringer(
            1,
            G1,
            G3,
            100000000
        )

        skrivTilTestdatafil(konteringer.subList(1, 3), "Oppdatering på gebyr for skyldner")
    }

    @Test
    @Order(3)
    fun `skal opprette gebyr for mottaker`() {
        val vedtakHendelse = hentFilOgSendPåKafka("gebyrMottaker.json", 4)

        val kontering = assertVedOpprettelseAvEngangsbeløp(
            2,
            vedtakHendelse,
            EngangsbelopType.GEBYR_MOTTAKER,
            G1,
            Integer.valueOf(vedtakHendelse.engangsbelopListe!![0].delytelseId),
            FABM
        )

        skrivTilTestdatafil(listOf(kontering), "Gebyr for mottaker")
    }

    @Test
    @Order(4)
    fun `skal oppdatere gebyr for mottaker`() {
        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(2) != null
        }

        hentFilOgSendPåKafka("gebyrMottakerOppdatering.json", 6)

        val konteringer = assertVedOppdateringAvEngangsbeløpOgReturnerKonteringer(
            2,
            G1,
            G3,
            100000001
        )

        skrivTilTestdatafil(konteringer.subList(1, 3), "Oppdatering på gebyr for skyldner")
    }

    @Test
    @Order(5)
    fun `skal opprette særtilskudd`() {
        val vedtakHendelse = hentFilOgSendPåKafka("særtilskudd.json", 7)

        val kontering = assertVedOpprettelseAvEngangsbeløp(
            3,
            vedtakHendelse,
            EngangsbelopType.SAERTILSKUDD,
            E1,
            100000002,
            EN
        )

        skrivTilTestdatafil(listOf(kontering), "Særtilskudd")
    }

    @Test
    @Order(6)
    fun `skal oppdatere særtilskudd`() {
        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(3) != null
        }

        hentFilOgSendPåKafka("særtilskuddOppdatering.json", 9)

        val konteringer = assertVedOppdateringAvEngangsbeløpOgReturnerKonteringer(
            3,
            E1,
            E3,
            100000003
        )

        skrivTilTestdatafil(konteringer.subList(1, 3), "Oppdatering på særtilskudd")
    }

    @Test
    @Order(7)
    fun `skal opprette tilbakekreving`() {
        val vedtakHendelse = hentFilOgSendPåKafka("tilbakekreving.json", 10)

        val kontering = assertVedOpprettelseAvEngangsbeløp(
            4,
            vedtakHendelse,
            EngangsbelopType.TILBAKEKREVING,
            H1,
            Integer.valueOf(vedtakHendelse.engangsbelopListe!![0].delytelseId),
            EN
        )

        skrivTilTestdatafil(listOf(kontering), "Tilbakekreving")
    }

    @Test
    @Order(8)
    fun `skal oppdatere tilbakekreving`() {
        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(4) != null
        }

        hentFilOgSendPåKafka("tilbakekrevingOppdatering.json", 12)

        val konteringer = assertVedOppdateringAvEngangsbeløpOgReturnerKonteringer(
            4,
            H1,
            H3,
            100000004
        )

        skrivTilTestdatafil(konteringer.subList(1, 3), "Oppdatering på tilbakekreving")
    }

    @Test
    @Order(9)
    fun `skal opprette ettergivelse`() {
        val vedtakHendelse = hentFilOgSendPåKafka("ettergivelse.json", 14)

        val kontering = assertVedOpprettelseAvEngangsbeløp(
            5,
            vedtakHendelse,
            EngangsbelopType.ETTERGIVELSE,
            K1,
            Integer.valueOf(vedtakHendelse.engangsbelopListe!![0].delytelseId),
            EN
        )

        skrivTilTestdatafil(listOf(kontering), "Ettergivelse")
    }

    @Test
    @Order(10)
    fun `skal opprette direkte oppgjør`() {
        val vedtakHendelse = hentFilOgSendPåKafka("direkteOppgjor.json", 15)

        val kontering = assertVedOpprettelseAvEngangsbeløp(
            7,
            vedtakHendelse,
            EngangsbelopType.DIREKTE_OPPGJOR,
            K2,
            Integer.valueOf(vedtakHendelse.engangsbelopListe!![0].delytelseId),
            EN
        )

        skrivTilTestdatafil(listOf(kontering), "Direkte oppgjør")
    }

    @Test
    @Order(11)
    fun `skal opprette ettergivelse tilbakekreving`() {
        val vedtakHendelse = hentFilOgSendPåKafka("ettergivelseTilbakekreving.json", 16)

        val kontering = assertVedOpprettelseAvEngangsbeløp(
            8,
            vedtakHendelse,
            EngangsbelopType.ETTERGIVELSE_TILBAKEKREVING,
            K3,
            Integer.valueOf(vedtakHendelse.engangsbelopListe!![0].delytelseId),
            EN
        )

        skrivTilTestdatafil(listOf(kontering), "Ettergivelse tilbakekreving")
    }

    val skyldnerIdent = PersonidentGenerator.genererFødselsnummer()
    val kravhaverIdent = PersonidentGenerator.genererFødselsnummer()

    @Test
    @Order(12)
    fun `skal opprette bidragsforskudd`() {
        val vedtakHendelse = hentFilOgSendPåKafka("bidragsforskudd.json", 31, skyldnerIdent, kravhaverIdent)

        val oppdrag = assertStønader(
            9,
            vedtakHendelse,
            FORSKUDD,
            3,
            3,
            A1,
            EN
        )

        val konteringer = hentAlleKonteringerForOppdrag(oppdrag)
        skrivTilTestdatafil(konteringer, "Bidragsforskudd")
    }

    @Test
    @Order(13)
    fun `skal oppdatere bidragsforskudd`() {
        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(9) != null
        }

        val vedtakHendelse = hentFilOgSendPåKafka(
            "bidragsforskuddOppdatering.json",
            41,
            skyldnerIdent,
            kravhaverIdent
        )

        val oppdrag = assertStønader(
            9,
            vedtakHendelse,
            FORSKUDD,
            4,
            1,
            A1,
            EN,
            A3
        )

        val konteringer = hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag)
        skrivTilTestdatafil(konteringer, "Oppdaterer bidragsforskudds med 50 øre og endrer til å slutte 2 mnd tidligere.")
    }

    val bmBidrag = PersonidentGenerator.genererFødselsnummer()
    val bpBidrag = PersonidentGenerator.genererFødselsnummer()
    val barn1Bidrag = PersonidentGenerator.genererFødselsnummer()
    val barn2Bidrag = PersonidentGenerator.genererFødselsnummer()

    @Test
    @Order(14)
    fun `skal opprette bidrag for to barn med gebyr til begge parter`() {
        val vedtakHendelse = hentFilOgSendPåKafka(
            filnavn = "barnebidrag.json",
            antallKonteringerTotalt = 55,
            bm = bmBidrag,
            bp = bpBidrag,
            barn1 = barn1Bidrag,
            barn2 = barn2Bidrag
        )

        val oppdrag1 = assertStønader(
            10,
            vedtakHendelse,
            BIDRAG,
            2,
            2,
            B1,
            EN
        )

        val oppdrag2 = assertStønader(
            11,
            vedtakHendelse,
            BIDRAG,
            2,
            2,
            B1,
            EN,
            stonadsendringIndex = 1
        )

        val gebyrBp = assertVedOpprettelseAvEngangsbeløp(
            12,
            vedtakHendelse,
            EngangsbelopType.GEBYR_SKYLDNER,
            G1,
            Integer.valueOf(vedtakHendelse.engangsbelopListe!![0].delytelseId),
            FABP
        )

        val gebyrBm = assertVedOpprettelseAvEngangsbeløp(
            13,
            vedtakHendelse,
            EngangsbelopType.GEBYR_MOTTAKER,
            G1,
            Integer.valueOf(vedtakHendelse.engangsbelopListe!![1].delytelseId),
            FABM,
            engangsbeløpIndex = 1
        )

        skrivTilTestdatafil(hentAlleKonteringerForOppdrag(oppdrag1), "Barnebidrag for barn 1")
        skrivTilTestdatafil(hentAlleKonteringerForOppdrag(oppdrag2), "Barnebidrag for barn 2")
        skrivTilTestdatafil(listOf(gebyrBp), "Gebyr til BP for barnebidrag")
        skrivTilTestdatafil(listOf(gebyrBm), "Gebyr til BM for barnebidrag")
    }

    @Test
    @Order(15)
    fun `skal oppdatere bidrag`() {
        val vedtakHendelse = hentFilOgSendPåKafka(
            filnavn = "barnebidragOppdatering.json",
            antallKonteringerTotalt = 71,
            bm = bmBidrag,
            bp = bpBidrag,
            barn1 = barn1Bidrag,
            barn2 = barn2Bidrag
        )

        val oppdrag1 = assertStønader(
            10, vedtakHendelse, BIDRAG, 3, 1, B1, EN, B3, 0
        )

        skrivTilTestdatafil(
            hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag1),
            "Oppdaterer barnebidrag for barn 1 med 10kr."
        )

        val oppdrag2 = assertStønader(
            11, vedtakHendelse, BIDRAG, 3, 1, B1, EN, B3, 1
        )

        skrivTilTestdatafil(
            hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag2),
            "Oppdaterer barnebidrag for barn 2 med 10kr."
        )
    }

    val bpOppfostring = PersonidentGenerator.genererFødselsnummer()
    val barn1Oppfostring = PersonidentGenerator.genererFødselsnummer()
    val barn2Oppfostring = PersonidentGenerator.genererFødselsnummer()

    @Test
    @Order(16)
    fun `skal opprette oppfostringsbidrag`() {
        val vedtakHendelse = hentFilOgSendPåKafka(
            "oppfostringsbidrag.json",
            87,
            bp = bpOppfostring,
            barn1 = barn1Oppfostring,
            barn2 = barn2Oppfostring
        )

        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(14) != null
        }

        val oppdrag1 = assertStønader(
            14,
            vedtakHendelse,
            OPPFOSTRINGSBIDRAG,
            1,
            1,
            B1,
            EN
        )

        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(15) != null
        }

        val oppdrag2 = assertStønader(
            15,
            vedtakHendelse,
            OPPFOSTRINGSBIDRAG,
            1,
            1,
            B1,
            EN
        )

        skrivTilTestdatafil(hentAlleKonteringerForOppdrag(oppdrag1), "Oppfostringsbidrag for barn 1")
        skrivTilTestdatafil(hentAlleKonteringerForOppdrag(oppdrag2), "Oppfostringsbidrag for barn 2")
    }

    @Test
    @Order(17)
    fun `skal oppdatere oppfostringsbidrag`() {
        val vedtakHendelse = hentFilOgSendPåKafka(
            filnavn = "oppfostringsbidragOppdatering.json",
            antallKonteringerTotalt = 119,
            bp = bpOppfostring,
            barn1 = barn1Oppfostring,
            barn2 = barn2Oppfostring
        )

        val oppdrag1 = assertStønader(
            14, vedtakHendelse, OPPFOSTRINGSBIDRAG, 2, 1, B1, EN, B3, 0
        )

        skrivTilTestdatafil(
            hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag1),
            "Oppdaterer oppfostringsbidrag for barn 1 med 100kr."
        )

        val oppdrag2 = assertStønader(
            15, vedtakHendelse, OPPFOSTRINGSBIDRAG, 2, 1, B1, EN, B3, 1
        )

        skrivTilTestdatafil(
            hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag2),
            "Oppdaterer oppfostringsbidrag for barn 2 med 100kr."
        )
    }

    @Test
    @Order(18)
    fun `skal opprette 18 års bidrag`() {
        val vedtakHendelse = hentFilOgSendPåKafka(
            "18årsbidrag.json",
            124,
            bp = skyldnerIdEktefelleBidrag,
            kravhaverIdent = kravhaverIdEktefellebidrag
        )

        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(16) != null
        }

        val oppdrag = assertStønader(
            16,
            vedtakHendelse,
            BIDRAG18AAR,
            3,
            3,
            D1,
            EN
        )

        skrivTilTestdatafil(hentAlleKonteringerForOppdrag(oppdrag), "18 års bidrag")
    }

    @Test
    @Order(19)
    fun `skal oppdatere 18 års bidrag`() {
        val vedtakHendelse = hentFilOgSendPåKafka(
            filnavn = "18årsbidragOppdatering.json",
            antallKonteringerTotalt = 135,
            bp = skyldnerIdEktefelleBidrag,
            kravhaverIdent = kravhaverIdEktefellebidrag
        )

        val oppdrag = assertStønader(
            16,
            vedtakHendelse,
            BIDRAG18AAR,
            4,
            1,
            D1,
            EN,
            D3
        )

        skrivTilTestdatafil(
            hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag),
            "Oppdaterer 18 års bidrag med 1 mnd lenger varighet, til å starte 1 mnd før og +100kr."
        )
    }

    val skyldnerIdEktefelleBidrag = PersonidentGenerator.genererFødselsnummer()
    val kravhaverIdEktefellebidrag = PersonidentGenerator.genererFødselsnummer()

    @Test
    @Order(20)
    fun `skal opprette ektefellebidrag`() {
        val vedtakHendelse = hentFilOgSendPåKafka(
            "ektefellebidrag.json",
            156,
            bp = skyldnerIdEktefelleBidrag,
            kravhaverIdent = kravhaverIdEktefellebidrag
        )

        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(17) != null
        }

        val oppdrag = assertStønader(
            17,
            vedtakHendelse,
            EKTEFELLEBIDRAG,
            2,
            2,
            F1,
            EN
        )

        skrivTilTestdatafil(hentAlleKonteringerForOppdrag(oppdrag), "Ektefellebidrag")
    }

    @Test
    @Order(21)
    fun `skal oppdatere ektefellebidrag`() {
        val vedtakHendelse = hentFilOgSendPåKafka(
            filnavn = "ektefellebidragOppdatering.json",
            antallKonteringerTotalt = 166,
            bp = skyldnerIdEktefelleBidrag,
            kravhaverIdent = kravhaverIdEktefellebidrag
        )

        val oppdrag = assertStønader(
            17,
            vedtakHendelse,
            EKTEFELLEBIDRAG,
            3,
            1,
            F1,
            EN,
            F3
        )

        skrivTilTestdatafil(
            hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag),
            "Oppdaterer ektefellebidrag med 1000kr fra 2022-02-01."
        )
    }

    @Test
    @Order(22)
    fun `skal opprette motregning`() {
        val vedtakHendelse = hentFilOgSendPåKafka(
            "motregning.json",
            177
        )

        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(18) != null
        }

        val oppdrag = assertStønader(
            18,
            vedtakHendelse,
            MOTREGNING,
            1,
            1,
            I1,
            EN
        )

        skrivTilTestdatafil(hentAlleKonteringerForOppdrag(oppdrag), "Motregning")
    }

    private fun assertStønader(
        oppdragId: Int,
        vedtakHendelse: VedtakHendelse,
        stønadstype: StonadType,
        antallOppdragsperioder: Int,
        antallOpprettetIGjeldendeFil: Int,
        forventetTransaksjonskode: Transaksjonskode,
        forventetSøknadstype: Søknadstype,
        forventetKorreksjonskode: Transaksjonskode? = null,
        stonadsendringIndex: Int = 0
    ): Oppdrag {
        val oppdrag = persistenceService.hentOppdrag(oppdragId) ?: error("Det finnes ingen oppdrag med angitt oppdragsId: $oppdragId")

        oppdrag.stønadType shouldBe stønadstype.name
        oppdrag.oppdragsperioder.size shouldBe antallOppdragsperioder
        oppdrag.sakId shouldBe vedtakHendelse.stonadsendringListe!![stonadsendringIndex].sakId
        oppdrag.oppdragsperioder.subList(antallOppdragsperioder - antallOpprettetIGjeldendeFil, antallOppdragsperioder)
            .forEachIndexed { i: Int, oppdragsperiode: Oppdragsperiode ->
                oppdragsperiode.vedtaksdato shouldBe vedtakHendelse.vedtakTidspunkt.toLocalDate()
                oppdragsperiode.vedtakId shouldBe vedtakHendelse.id
                oppdragsperiode.eksternReferanse shouldBe vedtakHendelse.stonadsendringListe!![stonadsendringIndex].eksternReferanse
                oppdragsperiode.opprettetAv shouldBe vedtakHendelse.opprettetAv
                oppdragsperiode.mottakerIdent shouldBe vedtakHendelse.stonadsendringListe!![stonadsendringIndex].mottakerId
                oppdragsperiode.delytelseId shouldNotBe null
                oppdragsperiode.periodeFra shouldBe vedtakHendelse.stonadsendringListe!![stonadsendringIndex].periodeListe[i].fomDato
                oppdragsperiode.periodeTil shouldBe vedtakHendelse.stonadsendringListe!![stonadsendringIndex].periodeListe[i].tilDato
                oppdragsperiode.beløp shouldBe vedtakHendelse.stonadsendringListe!![stonadsendringIndex].periodeListe[i].belop
                oppdragsperiode.valuta shouldBe vedtakHendelse.stonadsendringListe!![stonadsendringIndex].periodeListe[i].valutakode

                val månederForKontering = finnAlleMånederForKonteringer(
                    vedtakHendelse.stonadsendringListe!![stonadsendringIndex].periodeListe[i].fomDato,
                    vedtakHendelse.stonadsendringListe!![stonadsendringIndex].periodeListe[i].tilDato
                )

                oppdragsperiode.konteringer.size shouldBe månederForKontering.size
                oppdragsperiode.konteringer.forEach { kontering ->
                    kontering.transaksjonskode shouldBeIn listOf(forventetTransaksjonskode.name, forventetKorreksjonskode?.name)
                    kontering.søknadType shouldBe forventetSøknadstype.name
                    kontering.overføringsperiode shouldBeIn månederForKontering
                }
            }
        return oppdrag
    }

    private fun finnAlleMånederForKonteringer(fraDato: LocalDate, tilDato: LocalDate?): List<String> {
        val yearMonths = mutableListOf<String>()
        val sluttDato = if ((tilDato != null) && !tilDato.isAfter(PÅLØPSDATO)) tilDato else PÅLØPSDATO.plusMonths(1)
        var currentDato = fraDato
        while (currentDato.isBefore(sluttDato)) {
            yearMonths.add(YearMonth.from(currentDato).toString())
            currentDato = currentDato.plusMonths(1)
        }
        return yearMonths
    }

    private fun assertVedOppdateringAvEngangsbeløpOgReturnerKonteringer(
        oppdragId: Int,
        forventetTransaksjonskode: Transaksjonskode,
        forventetKorreksjonskode: Transaksjonskode,
        forventetDelytelsesId: Int
    ): List<Kontering> {
        val oppdrag = persistenceService.hentOppdrag(oppdragId) ?: error("Det finnes ingen oppdrag med angitt oppdragsId: $oppdragId")

        oppdrag.oppdragsperioder shouldHaveSize 2
        oppdrag.oppdragsperioder[0].beløp shouldNotBe oppdrag.oppdragsperioder[1].beløp

        oppdrag.oppdragsperioder[0].konteringer shouldHaveSize 2
        oppdrag.oppdragsperioder[0].konteringer[0].transaksjonskode shouldBe forventetTransaksjonskode.name
        oppdrag.oppdragsperioder[0].konteringer[0].type shouldBe NY.name
        oppdrag.oppdragsperioder[0].konteringer[1].transaksjonskode shouldBe forventetKorreksjonskode.name
        oppdrag.oppdragsperioder[0].konteringer[1].type shouldBe ENDRING.name
        oppdrag.oppdragsperioder[1].konteringer shouldHaveSize 1
        oppdrag.oppdragsperioder[1].konteringer[0].transaksjonskode shouldBe forventetTransaksjonskode.name
        oppdrag.oppdragsperioder[1].konteringer[0].type shouldBe ENDRING.name
        oppdrag.oppdragsperioder[1].delytelseId shouldBe forventetDelytelsesId

        val konteringer = hentAlleKonteringerForOppdrag(oppdrag)
        return konteringer
    }

    private fun hentFilOgSendPåKafka(
        filnavn: String,
        antallKonteringerTotalt: Int,
        kravhaverIdent: String = PersonidentGenerator.genererFødselsnummer(),
        mottaker: String = PersonidentGenerator.genererFødselsnummer(),
        bm: String = PersonidentGenerator.genererFødselsnummer(),
        bp: String = PersonidentGenerator.genererFødselsnummer(),
        barn1: String = PersonidentGenerator.genererFødselsnummer(),
        barn2: String = PersonidentGenerator.genererFødselsnummer()
    ): VedtakHendelse {
        val vedtakFilString = leggInnGenererteIdenter(hentTestfil(filnavn), kravhaverIdent, mottaker, bm, bp, barn1, barn2)

        kafkaTemplate.send(topic, vedtakFilString)

        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.konteringRepository.findAll().size == antallKonteringerTotalt
        }
        return objectmapper.readValue(vedtakFilString, VedtakHendelse::class.java)
    }

    private fun assertVedOpprettelseAvEngangsbeløp(
        oppdragId: Int,
        vedtakHendelse: VedtakHendelse,
        forventetEngangsbeløpType: EngangsbelopType,
        forventetTransaksjonskode: Transaksjonskode,
        forventetDelytelsesId: Int,
        søknadstype: Søknadstype,
        engangsbeløpIndex: Int = 0
    ): Kontering {
        val oppdrag = persistenceService.hentOppdrag(oppdragId) ?: error("Det finnes ingen oppdrag med angitt oppdragsId: $oppdragId")
        assertSoftly {
            oppdrag.stønadType shouldBe forventetEngangsbeløpType.name
            oppdrag.sakId shouldBe vedtakHendelse.engangsbelopListe!![engangsbeløpIndex].sakId
        }

        val oppdragsperiode = oppdrag.oppdragsperioder.first()
        assertSoftly {
            oppdragsperiode.referanse shouldBe vedtakHendelse.engangsbelopListe!![engangsbeløpIndex].referanse
            oppdragsperiode.oppdrag shouldBeSameInstanceAs oppdrag
            oppdragsperiode.vedtakId shouldBe vedtakHendelse.id
            oppdragsperiode.beløp shouldBe vedtakHendelse.engangsbelopListe!![engangsbeløpIndex].belop
            oppdragsperiode.valuta shouldBe vedtakHendelse.engangsbelopListe!![engangsbeløpIndex].valutakode
            oppdragsperiode.vedtaksdato shouldBe vedtakHendelse.vedtakTidspunkt.toLocalDate()
            oppdragsperiode.periodeFra shouldBe vedtakHendelse.vedtakTidspunkt.toLocalDate().withDayOfMonth(1)
            oppdragsperiode.periodeTil shouldBe vedtakHendelse.vedtakTidspunkt.toLocalDate().plusMonths(1).withDayOfMonth(1)
            oppdragsperiode.opprettetAv shouldBe vedtakHendelse.opprettetAv
            oppdragsperiode.delytelseId shouldBe forventetDelytelsesId
        }

        val kontering = hentAlleKonteringerForOppdrag(oppdrag).first()
        assertSoftly {
            kontering.transaksjonskode shouldBe forventetTransaksjonskode.name
            kontering.overføringsperiode shouldBe YearMonth.from(vedtakHendelse.vedtakTidspunkt.toLocalDate()).toString()
            kontering.type shouldBe NY.name
            kontering.søknadType shouldBe søknadstype.name
        }
        return kontering
    }

    private fun skrivTilTestdatafil(konteringer: List<Kontering>, kommentar: String) {
        val skattKravRequest = kravService.opprettKravKonteringListe(konteringer)
        file.write("\n// $kommentar\n".toByteArray())
        file.write(objectmapper.writerWithDefaultPrettyPrinter().writeValueAsString(skattKravRequest).toByteArray())
    }

    private fun leggInnGenererteIdenter(
        vedtakFil: String,
        kravhaverIdent: String,
        mottaker: String,
        bm: String,
        bp: String,
        barn1: String,
        barn2: String
    ): String {
        return vedtakFil.replace("\"skyldnerId\": \"\"", "\"skyldnerId\" : \"${PersonidentGenerator.genererFødselsnummer()}\"")
            .replace("\"kravhaverId\": \"\"", "\"kravhaverId\" : \"$kravhaverIdent\"")
            .replace("\"mottakerId\": \"\"", "\"mottakerId\" : \"$mottaker\"")
            .replace("\"skyldnerId\": \"BP\"", "\"skyldnerId\" : \"$bp\"")
            .replace("\"skyldnerId\": \"BM\"", "\"skyldnerId\" : \"$bm\"")
            .replace("\"kravhaverId\": \"BARN1\"", "\"kravhaverId\" : \"$barn1\"")
            .replace("\"kravhaverId\": \"BARN2\"", "\"kravhaverId\" : \"$barn2\"")
            .replace("\"mottakerId\": \"BM\"", "\"mottakerId\" : \"$bm\"")
    }

    private fun hentAlleKonteringerForOppdrag(oppdrag: Oppdrag): List<Kontering> {
        val konteringer = mutableListOf<Kontering>()
        oppdrag.oppdragsperioder.forEach { oppdragsperiode ->
            oppdragsperiode.konteringer.forEach { kontering ->
                konteringer.add(kontering)
            }
        }
        return konteringer
    }

    private fun hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag: Oppdrag): List<Kontering> {
        val konteringer = mutableListOf<Kontering>()

        oppdrag.oppdragsperioder[oppdrag.oppdragsperioder.size - 2].konteringer.forEach { kontering ->
            if (Transaksjonskode.valueOf(kontering.transaksjonskode).korreksjonskode == null) {
                konteringer.add(kontering)
            }
        }
        oppdrag.oppdragsperioder.last().konteringer.forEach { kontering ->
            konteringer.add(kontering)
        }
        return konteringer
    }

    private fun hentTestfil(filnavn: String): String {
        return String(javaClass.classLoader.getResourceAsStream("${HENDELSE_FILMAPPE}$filnavn")!!.readAllBytes())
    }
}
