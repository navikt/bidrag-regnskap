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
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.domene.enums.regnskap.Søknadstype
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.domene.enums.regnskap.Type
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.regnskap.BidragRegnskapLocal
import no.nav.bidrag.regnskap.consumer.KravApiWireMock
import no.nav.bidrag.regnskap.consumer.PersonApiWireMock
import no.nav.bidrag.regnskap.consumer.SakApiWireMock
import no.nav.bidrag.regnskap.maskinporten.MaskinportenWireMock
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.service.KravService
import no.nav.bidrag.regnskap.service.PersistenceService
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
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
        private var personApiWireMock: PersonApiWireMock = PersonApiWireMock()

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
        TestData.opprettPåløp(
            forPeriode = YearMonth.from(PÅLØPSDATO).toString(),
            fullførtTidspunkt = LocalDateTime.now(),
        )

    private val objectmapper =
        jacksonObjectMapper().registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

    @BeforeAll
    fun beforeAll() {
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
        personApiWireMock.personidentMedNoBody()
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
            100000001,
            vedtakHendelse,
            Engangsbeløptype.GEBYR_SKYLDNER,
            Transaksjonskode.G1,
            Integer.valueOf(vedtakHendelse.engangsbeløpListe!![0].delytelseId),
            Søknadstype.FABP,
        )

        skrivTilTestdatafil(listOf(kontering), "Gebyr for skyldner")
    }

    @Test
    @Order(2)
    fun `skal oppdatere gebyr for skyldner`() {
        hentFilOgSendPåKafka("gebyrSkyldnerOppdatering.json", 3)

        val konteringer = assertVedOppdateringAvEngangsbeløpOgReturnerKonteringer(
            100000001,
            Transaksjonskode.G1,
            Transaksjonskode.G3,
            100000000,
        )

        skrivTilTestdatafil(konteringer.subList(1, 3), "Oppdatering på gebyr for skyldner")
    }

    @Test
    @Order(3)
    fun `skal opprette gebyr for mottaker`() {
        val vedtakHendelse = hentFilOgSendPåKafka("gebyrMottaker.json", 4)

        val kontering = assertVedOpprettelseAvEngangsbeløp(
            100000002,
            vedtakHendelse,
            Engangsbeløptype.GEBYR_MOTTAKER,
            Transaksjonskode.G1,
            Integer.valueOf(vedtakHendelse.engangsbeløpListe!![0].delytelseId),
            Søknadstype.FABM,
        )

        skrivTilTestdatafil(listOf(kontering), "Gebyr for mottaker")
    }

    @Test
    @Order(4)
    fun `skal oppdatere gebyr for mottaker`() {
        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(100000002) != null
        }

        hentFilOgSendPåKafka("gebyrMottakerOppdatering.json", 6)

        val konteringer = assertVedOppdateringAvEngangsbeløpOgReturnerKonteringer(
            100000002,
            Transaksjonskode.G1,
            Transaksjonskode.G3,
            100000001,
        )

        skrivTilTestdatafil(konteringer.subList(1, 3), "Oppdatering på gebyr for skyldner")
    }

    @Test
    @Order(5)
    fun `skal opprette særtilskudd`() {
        val vedtakHendelse = hentFilOgSendPåKafka("særtilskudd.json", 7)

        val kontering = assertVedOpprettelseAvEngangsbeløp(
            100000003,
            vedtakHendelse,
            Engangsbeløptype.SÆRTILSKUDD,
            Transaksjonskode.E1,
            100000002,
            Søknadstype.EN,
        )

        skrivTilTestdatafil(listOf(kontering), "Særtilskudd")
    }

    @Test
    @Order(6)
    fun `skal oppdatere særtilskudd`() {
        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(100000003) != null
        }

        hentFilOgSendPåKafka("særtilskuddOppdatering.json", 9)

        val konteringer = assertVedOppdateringAvEngangsbeløpOgReturnerKonteringer(
            100000003,
            Transaksjonskode.E1,
            Transaksjonskode.E3,
            100000003,
        )

        skrivTilTestdatafil(konteringer.subList(1, 3), "Oppdatering på særtilskudd")
    }

    @Test
    @Order(7)
    fun `skal opprette tilbakekreving`() {
        val vedtakHendelse = hentFilOgSendPåKafka("tilbakekreving.json", 10)

        val kontering = assertVedOpprettelseAvEngangsbeløp(
            100000004,
            vedtakHendelse,
            Engangsbeløptype.TILBAKEKREVING,
            Transaksjonskode.H1,
            Integer.valueOf(vedtakHendelse.engangsbeløpListe!![0].delytelseId),
            Søknadstype.EN,
        )

        skrivTilTestdatafil(listOf(kontering), "Tilbakekreving")
    }

    @Test
    @Order(8)
    fun `skal oppdatere tilbakekreving`() {
        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(100000004) != null
        }

        hentFilOgSendPåKafka("tilbakekrevingOppdatering.json", 12)

        val konteringer = assertVedOppdateringAvEngangsbeløpOgReturnerKonteringer(
            100000004,
            Transaksjonskode.H1,
            Transaksjonskode.H3,
            100000004,
        )

        skrivTilTestdatafil(konteringer.subList(1, 3), "Oppdatering på tilbakekreving")
    }

    @Test
    @Order(9)
    fun `skal opprette ettergivelse`() {
        val vedtakHendelse = hentFilOgSendPåKafka("ettergivelse.json", 14)

        val kontering = assertVedOpprettelseAvEngangsbeløp(
            100000005,
            vedtakHendelse,
            Engangsbeløptype.ETTERGIVELSE,
            Transaksjonskode.K1,
            Integer.valueOf(vedtakHendelse.engangsbeløpListe!![0].delytelseId),
            Søknadstype.EN,
        )

        skrivTilTestdatafil(listOf(kontering), "Ettergivelse")
    }

    @Test
    @Order(10)
    fun `skal opprette direkte oppgjør`() {
        val vedtakHendelse = hentFilOgSendPåKafka("direkteOppgjor.json", 15)

        val kontering = assertVedOpprettelseAvEngangsbeløp(
            100000007,
            vedtakHendelse,
            Engangsbeløptype.DIREKTE_OPPGJØR,
            Transaksjonskode.K2,
            Integer.valueOf(vedtakHendelse.engangsbeløpListe!![0].delytelseId),
            Søknadstype.EN,
        )

        skrivTilTestdatafil(listOf(kontering), "Direkte oppgjør")
    }

    @Test
    @Order(11)
    fun `skal opprette ettergivelse tilbakekreving`() {
        val vedtakHendelse = hentFilOgSendPåKafka("ettergivelseTilbakekreving.json", 16)

        val kontering = assertVedOpprettelseAvEngangsbeløp(
            100000008,
            vedtakHendelse,
            Engangsbeløptype.ETTERGIVELSE_TILBAKEKREVING,
            Transaksjonskode.K3,
            Integer.valueOf(vedtakHendelse.engangsbeløpListe!![0].delytelseId),
            Søknadstype.EN,
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
            100000009,
            vedtakHendelse,
            Stønadstype.FORSKUDD,
            3,
            3,
            Transaksjonskode.A1,
            Søknadstype.EN,
        )

        val konteringer = hentAlleKonteringerForOppdrag(oppdrag)
        skrivTilTestdatafil(konteringer, "Bidragsforskudd")
    }

    @Test
    @Order(13)
    fun `skal oppdatere bidragsforskudd`() {
        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(100000009) != null
        }

        val vedtakHendelse = hentFilOgSendPåKafka(
            "bidragsforskuddOppdatering.json",
            41,
            skyldnerIdent,
            kravhaverIdent,
        )

        val oppdrag = assertStønader(
            100000009,
            vedtakHendelse,
            Stønadstype.FORSKUDD,
            4,
            1,
            Transaksjonskode.A1,
            Søknadstype.EN,
            Transaksjonskode.A3,
        )

        val konteringer = hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag)
        skrivTilTestdatafil(
            konteringer,
            "Oppdaterer bidragsforskudds med 50 øre og endrer til å slutte 2 mnd tidligere.",
        )
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
            barn2 = barn2Bidrag,
        )

        val oppdrag1 = assertStønader(
            100000010,
            vedtakHendelse,
            Stønadstype.BIDRAG,
            2,
            2,
            Transaksjonskode.B1,
            Søknadstype.EN,
        )

        val oppdrag2 = assertStønader(
            100000011,
            vedtakHendelse,
            Stønadstype.BIDRAG,
            2,
            2,
            Transaksjonskode.B1,
            Søknadstype.EN,
            stonadsendringIndex = 1,
        )

        val gebyrBp = assertVedOpprettelseAvEngangsbeløp(
            100000012,
            vedtakHendelse,
            Engangsbeløptype.GEBYR_SKYLDNER,
            Transaksjonskode.G1,
            Integer.valueOf(vedtakHendelse.engangsbeløpListe!![0].delytelseId),
            Søknadstype.FABP,
        )

        val gebyrBm = assertVedOpprettelseAvEngangsbeløp(
            100000013,
            vedtakHendelse,
            Engangsbeløptype.GEBYR_MOTTAKER,
            Transaksjonskode.G1,
            Integer.valueOf(vedtakHendelse.engangsbeløpListe!![1].delytelseId),
            Søknadstype.FABM,
            engangsbeløpIndex = 1,
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
            barn2 = barn2Bidrag,
        )

        val oppdrag1 = assertStønader(
            100000010,
            vedtakHendelse,
            Stønadstype.BIDRAG,
            3,
            1,
            Transaksjonskode.B1,
            Søknadstype.EN,
            Transaksjonskode.B3, 0,
        )

        skrivTilTestdatafil(
            hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag1),
            "Oppdaterer barnebidrag for barn 1 med 10kr.",
        )

        val oppdrag2 = assertStønader(
            100000011,
            vedtakHendelse,
            Stønadstype.BIDRAG,
            3,
            1,
            Transaksjonskode.B1,
            Søknadstype.EN,
            Transaksjonskode.B3,
            1,
        )

        skrivTilTestdatafil(
            hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag2),
            "Oppdaterer barnebidrag for barn 2 med 10kr.",
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
            barn2 = barn2Oppfostring,
        )

        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(100000014) != null
        }

        val oppdrag1 = assertStønader(
            100000014,
            vedtakHendelse,
            Stønadstype.OPPFOSTRINGSBIDRAG,
            1,
            1,
            Transaksjonskode.B1,
            Søknadstype.EN,
        )

        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(100000015) != null
        }

        val oppdrag2 = assertStønader(
            100000015,
            vedtakHendelse,
            Stønadstype.OPPFOSTRINGSBIDRAG,
            1,
            1,
            Transaksjonskode.B1,
            Søknadstype.EN,
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
            barn2 = barn2Oppfostring,
        )

        val oppdrag1 = assertStønader(
            100000014,
            vedtakHendelse,
            Stønadstype.OPPFOSTRINGSBIDRAG,
            2,
            1,
            Transaksjonskode.B1,
            Søknadstype.EN,
            Transaksjonskode.B3,
            0,
        )

        skrivTilTestdatafil(
            hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag1),
            "Oppdaterer oppfostringsbidrag for barn 1 med 100kr.",
        )

        val oppdrag2 = assertStønader(
            100000015,
            vedtakHendelse,
            Stønadstype.OPPFOSTRINGSBIDRAG,
            2,
            1,
            Transaksjonskode.B1,
            Søknadstype.EN,
            Transaksjonskode.B3,
            1,
        )

        skrivTilTestdatafil(
            hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag2),
            "Oppdaterer oppfostringsbidrag for barn 2 med 100kr.",
        )
    }

    val bidrag18årsMottaker = PersonidentGenerator.genererFødselsnummer()
    val bidrag18årsMottakerNy = PersonidentGenerator.genererFødselsnummer()

    @Test
    @Order(18)
    fun `skal opprette 18 års bidrag`() {
        val vedtakHendelse = hentFilOgSendPåKafka(
            "18årsbidrag.json",
            124,
            bp = skyldnerIdEktefelleBidrag,
            kravhaverIdent = kravhaverIdEktefellebidrag,
            mottaker = bidrag18årsMottaker,
        )

        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(100000016) != null
        }

        val oppdrag = assertStønader(
            100000016,
            vedtakHendelse,
            Stønadstype.BIDRAG18AAR,
            3,
            3,
            Transaksjonskode.D1,
            Søknadstype.EN,
            forventetMottaker = bidrag18årsMottaker,
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
            kravhaverIdent = kravhaverIdEktefellebidrag,
            mottaker = bidrag18årsMottakerNy,
        )

        val oppdrag = assertStønader(
            100000016,
            vedtakHendelse,
            Stønadstype.BIDRAG18AAR,
            4,
            1,
            Transaksjonskode.D1,
            Søknadstype.EN,
            Transaksjonskode.D3,
            forventetMottaker = bidrag18årsMottakerNy,
        )

        skrivTilTestdatafil(
            hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag),
            "Oppdaterer 18 års bidrag med 1 mnd lenger varighet, til å starte 1 mnd før og +100kr.",
        )
    }

    val skyldnerIdEktefelleBidrag = PersonidentGenerator.genererFødselsnummer()
    val kravhaverIdEktefellebidrag = PersonidentGenerator.genererFødselsnummer()
    val mottakerEktefellebidrag = PersonidentGenerator.genererFødselsnummer()
    val mottakerEktefellebidragNy = PersonidentGenerator.genererFødselsnummer()

    @Test
    @Order(20)
    fun `skal opprette ektefellebidrag`() {
        val vedtakHendelse = hentFilOgSendPåKafka(
            "ektefellebidrag.json",
            156,
            bp = skyldnerIdEktefelleBidrag,
            kravhaverIdent = kravhaverIdEktefellebidrag,
        )

        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(100000017) != null
        }

        val oppdrag = assertStønader(
            100000017,
            vedtakHendelse,
            Stønadstype.EKTEFELLEBIDRAG,
            2,
            2,
            Transaksjonskode.F1,
            Søknadstype.EN,
            forventetMottaker = mottakerEktefellebidrag,
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
            kravhaverIdent = kravhaverIdEktefellebidrag,
        )

        val oppdrag = assertStønader(
            100000017,
            vedtakHendelse,
            Stønadstype.EKTEFELLEBIDRAG,
            3,
            1,
            Transaksjonskode.F1,
            Søknadstype.EN,
            Transaksjonskode.F3,
            forventetMottaker = mottakerEktefellebidragNy,
        )

        skrivTilTestdatafil(
            hentAlleOppdaterteOgNyeKonteringerForOppdragVedOppdatering(oppdrag),
            "Oppdaterer ektefellebidrag med 1000kr fra 2022-02-01.",
        )
    }

    @Test
    @Order(22)
    fun `skal opprette motregning`() {
        val vedtakHendelse = hentFilOgSendPåKafka(
            "motregning.json",
            177,
        )

        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.hentOppdrag(100000018) != null
        }

        val oppdrag = assertStønader(
            100000018,
            vedtakHendelse,
            Stønadstype.MOTREGNING,
            1,
            1,
            Transaksjonskode.I1,
            Søknadstype.EN,
        )

        skrivTilTestdatafil(hentAlleKonteringerForOppdrag(oppdrag), "Motregning")
    }

    val endreRmBmBidrag = PersonidentGenerator.genererFødselsnummer()
    val endreRmBmNyBidrag = PersonidentGenerator.genererFødselsnummer()
    val endreRmBpBidrag = PersonidentGenerator.genererFødselsnummer()
    val endreRmBarn1Bidrag = PersonidentGenerator.genererFødselsnummer()

    @Test
    @Order(23)
    fun `skal endre rm`() {
        val vedtakHendelse = hentFilOgSendPåKafka(
            "endreRm.json",
            178,
            bm = endreRmBmBidrag,
            bp = endreRmBpBidrag,
            barn1 = endreRmBarn1Bidrag,
        )

        assertStønader(
            100000019,
            vedtakHendelse,
            Stønadstype.BIDRAG,
            1,
            1,
            Transaksjonskode.B1,
            Søknadstype.EN,
        )
    }

    @Test
    @Order(24)
    fun `skal endre rm oppdatering`() {
        hentFilOgSendPåKafka(
            "endreRmOppdatering.json",
            178,
            bm = endreRmBmNyBidrag,
            bp = endreRmBpBidrag,
            barn1 = endreRmBarn1Bidrag,
        )

        val oppdrag = persistenceService.hentOppdrag(100000019)
        oppdrag?.mottakerIdent shouldBe endreRmBmNyBidrag
    }

    fun assertStønader(
        oppdragId: Int,
        vedtakHendelse: VedtakHendelse,
        stønadstype: Stønadstype,
        antallOppdragsperioder: Int,
        antallOpprettetIGjeldendeFil: Int,
        forventetTransaksjonskode: Transaksjonskode,
        forventetSøknadstype: Søknadstype,
        forventetKorreksjonskode: Transaksjonskode? = null,
        stonadsendringIndex: Int = 0,
        forventetMottaker: String? = null,
    ): Oppdrag {
        val oppdrag = persistenceService.hentOppdrag(oppdragId)
            ?: error("Det finnes ingen oppdrag med angitt oppdragsId: $oppdragId")

        oppdrag.stønadType shouldBe stønadstype.name
        oppdrag.oppdragsperioder.size shouldBe antallOppdragsperioder
        oppdrag.sakId shouldBe vedtakHendelse.stønadsendringListe!![stonadsendringIndex].sak.verdi

        oppdrag.oppdragsperioder.subList(antallOppdragsperioder - antallOpprettetIGjeldendeFil, antallOppdragsperioder)
            .forEachIndexed { i: Int, oppdragsperiode: Oppdragsperiode ->
                oppdragsperiode.vedtaksdato shouldBe vedtakHendelse.vedtakstidspunkt.toLocalDate()
                oppdragsperiode.vedtakId shouldBe vedtakHendelse.id
                oppdragsperiode.eksternReferanse shouldBe vedtakHendelse.stønadsendringListe!![stonadsendringIndex].eksternReferanse
                oppdragsperiode.opprettetAv shouldBe vedtakHendelse.opprettetAv
                oppdragsperiode.delytelseId shouldNotBe null
                oppdragsperiode.periodeFra shouldBe vedtakHendelse.stønadsendringListe!![stonadsendringIndex].periodeListe[i].periode.toDatoperiode().fom
                oppdragsperiode.periodeTil shouldBe vedtakHendelse.stønadsendringListe!![stonadsendringIndex].periodeListe[i].periode.toDatoperiode().til
                oppdragsperiode.beløp shouldBe vedtakHendelse.stønadsendringListe!![stonadsendringIndex].periodeListe[i].beløp
                oppdragsperiode.valuta shouldBe vedtakHendelse.stønadsendringListe!![stonadsendringIndex].periodeListe[i].valutakode

                val månederForKontering = finnAlleMånederForKonteringer(
                    vedtakHendelse.stønadsendringListe!![stonadsendringIndex].periodeListe[i].periode.toDatoperiode().fom,
                    vedtakHendelse.stønadsendringListe!![stonadsendringIndex].periodeListe[i].periode.toDatoperiode().til,
                )

                oppdragsperiode.konteringer.size shouldBe månederForKontering.size
                oppdragsperiode.konteringer.forEach { kontering ->
                    kontering.transaksjonskode shouldBeIn listOf(
                        forventetTransaksjonskode.name,
                        forventetKorreksjonskode?.name,
                    )
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
        forventetDelytelsesId: Int,
    ): List<Kontering> {
        val oppdrag = persistenceService.hentOppdrag(oppdragId)
            ?: error("Det finnes ingen oppdrag med angitt oppdragsId: $oppdragId")

        oppdrag.oppdragsperioder shouldHaveSize 2
        oppdrag.oppdragsperioder[0].beløp shouldNotBe oppdrag.oppdragsperioder[1].beløp

        oppdrag.oppdragsperioder[0].konteringer shouldHaveSize 2
        oppdrag.oppdragsperioder[0].konteringer[0].transaksjonskode shouldBe forventetTransaksjonskode.name
        oppdrag.oppdragsperioder[0].konteringer[0].type shouldBe Type.NY.name
        oppdrag.oppdragsperioder[0].konteringer[1].transaksjonskode shouldBe forventetKorreksjonskode.name
        oppdrag.oppdragsperioder[0].konteringer[1].type shouldBe Type.ENDRING.name
        oppdrag.oppdragsperioder[1].konteringer shouldHaveSize 1
        oppdrag.oppdragsperioder[1].konteringer[0].transaksjonskode shouldBe forventetTransaksjonskode.name
        oppdrag.oppdragsperioder[1].konteringer[0].type shouldBe Type.ENDRING.name
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
        barn2: String = PersonidentGenerator.genererFødselsnummer(),
    ): VedtakHendelse {
        val vedtakFilString =
            leggInnGenererteIdenter(hentTestfil(filnavn), kravhaverIdent, mottaker, bm, bp, barn1, barn2)

        kafkaTemplate.send(topic, vedtakFilString)

        println(
            "$filnavn blir nå behandlet. Antall opprettede konteringer totalt i db så langt: ${persistenceService.konteringRepository.findAll().size}",
        )

        await().atMost(TEN_SECONDS).until {
            return@until persistenceService.konteringRepository.findAll().size == antallKonteringerTotalt
        }
        return objectmapper.readValue(vedtakFilString, VedtakHendelse::class.java)
    }

    private fun assertVedOpprettelseAvEngangsbeløp(
        oppdragId: Int,
        vedtakHendelse: VedtakHendelse,
        forventetEngangsbeløpType: Engangsbeløptype,
        forventetTransaksjonskode: Transaksjonskode,
        forventetDelytelsesId: Int,
        søknadstype: Søknadstype,
        engangsbeløpIndex: Int = 0,
    ): Kontering {
        val oppdrag = persistenceService.hentOppdrag(oppdragId)
            ?: error("Det finnes ingen oppdrag med angitt oppdragsId: $oppdragId")
        assertSoftly {
            oppdrag.stønadType shouldBe forventetEngangsbeløpType.name
            oppdrag.sakId shouldBe vedtakHendelse.engangsbeløpListe!![engangsbeløpIndex].sak.verdi
        }

        val oppdragsperiode = oppdrag.oppdragsperioder.first()
        assertSoftly {
            oppdragsperiode.referanse shouldBe vedtakHendelse.engangsbeløpListe!![engangsbeløpIndex].referanse
            oppdragsperiode.oppdrag shouldBeSameInstanceAs oppdrag
            oppdragsperiode.vedtakId shouldBe vedtakHendelse.id
            oppdragsperiode.beløp shouldBe vedtakHendelse.engangsbeløpListe!![engangsbeløpIndex].beløp
            oppdragsperiode.valuta shouldBe vedtakHendelse.engangsbeløpListe!![engangsbeløpIndex].valutakode
            oppdragsperiode.vedtaksdato shouldBe vedtakHendelse.vedtakstidspunkt.toLocalDate()
            oppdragsperiode.periodeFra shouldBe vedtakHendelse.vedtakstidspunkt.toLocalDate().withDayOfMonth(1)
            oppdragsperiode.periodeTil shouldBe vedtakHendelse.vedtakstidspunkt.toLocalDate().plusMonths(1)
                .withDayOfMonth(1)
            oppdragsperiode.opprettetAv shouldBe vedtakHendelse.opprettetAv
            oppdragsperiode.delytelseId shouldBe forventetDelytelsesId
        }

        val kontering = hentAlleKonteringerForOppdrag(oppdrag).first()
        assertSoftly {
            kontering.transaksjonskode shouldBe forventetTransaksjonskode.name
            kontering.overføringsperiode shouldBe YearMonth.from(vedtakHendelse.vedtakstidspunkt.toLocalDate())
                .toString()
            kontering.type shouldBe Type.NY.name
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
        barn2: String,
    ): String {
        return vedtakFil.replace(
            "\"skyldner\": \"\"",
            "\"skyldner\" : \"${PersonidentGenerator.genererFødselsnummer()}\"",
        )
            .replace("\"kravhaver\": \"\"", "\"kravhaver\" : \"$kravhaverIdent\"")
            .replace("\"mottaker\": \"\"", "\"mottaker\" : \"$mottaker\"")
            .replace("\"skyldner\": \"BP\"", "\"skyldner\" : \"$bp\"")
            .replace("\"skyldner\": \"BM\"", "\"skyldner\" : \"$bm\"")
            .replace("\"kravhaver\": \"BARN1\"", "\"kravhaver\" : \"$barn1\"")
            .replace("\"kravhaver\": \"BARN2\"", "\"kravhaver\" : \"$barn2\"")
            .replace("\"mottaker\": \"BM\"", "\"mottaker\" : \"$bm\"")
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
