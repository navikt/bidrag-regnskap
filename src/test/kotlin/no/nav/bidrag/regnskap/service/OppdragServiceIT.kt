package no.nav.bidrag.regnskap.service

import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.regnskap.BidragRegnskapLocal
import no.nav.bidrag.regnskap.consumer.SakApiWireMock
import no.nav.bidrag.regnskap.dto.vedtak.Hendelse
import no.nav.bidrag.regnskap.dto.vedtak.Periode
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.time.measureTime

@Disabled("Denne er for å teste hastighet ved opprettelse av oppdrag fra hendelser. Bør ikke kjøres fast.")
@Transactional
@ActiveProfiles("test")
@EnableMockOAuth2Server
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [BidragRegnskapLocal::class])
@EmbeddedKafka(partitions = 1, brokerProperties = ["listeners=PLAINTEXT://localhost:9092", "port=9092"])
class OppdragServiceIT {
    companion object {

        val antallHendelser = 600

        private var sakApiWireMock: SakApiWireMock = SakApiWireMock()

        private var postgreSqlDb = PostgreSQLContainer("postgres:latest").apply {
            withDatabaseName("bidrag-regnskap")
            withUsername("cloudsqliamuser")
            withPassword("admin")
            withEnv("reWriteBatchedInserts", "true")
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
    private lateinit var oppdragService: OppdragService

    @Autowired
    private lateinit var persistenceService: PersistenceService

    private var påløp: Påløp? = null

    @BeforeAll
    fun beforeAll() {
        påløp = lagrePåløp()
    }

    @BeforeEach
    fun beforeEach() {
        sakApiWireMock.sakMedGyldigResponse()
    }

    @Test
    fun `skal ta tiden på opprettelse av hendelser`() {
        val tidsbruk = measureTime {
            opprettHendelser(antallHendelser).forEach {
                oppdragService.lagreHendelse(it)
            }
        }
        val tidsbrukISekunder = tidsbruk.inWholeMilliseconds.toDouble() / 1000
        println("Det tok $tidsbrukISekunder sekunder å opprette $antallHendelser hendelser.")
    }

    @Suppress("SameParameterValue")
    private fun opprettHendelser(antallHendelser: Int): List<Hendelse> {
        val hendelser: MutableList<Hendelse> = mutableListOf()
        for (i in 0..<antallHendelser) {
            hendelser.add(
                Hendelse(
                    type = "BIDRAG",
                    vedtakType = Vedtakstype.FASTSETTELSE,
                    kravhaverIdent = PersonidentGenerator.genererFødselsnummer(),
                    skyldnerIdent = PersonidentGenerator.genererFødselsnummer(),
                    mottakerIdent = PersonidentGenerator.genererFødselsnummer(),
                    sakId = Random.nextInt().toString(),
                    vedtakId = Random.nextInt(),
                    vedtakDato = LocalDate.now(),
                    opprettetAv = "TEST",
                    enhetsnummer = Enhetsnummer("1234"),
                    utsattTilDato = null,
                    eksternReferanse = null,
                    periodeListe = listOf(
                        Periode(
                            beløp = BigDecimal.valueOf(Random.nextDouble()),
                            valutakode = "NOK",
                            periodeFomDato = LocalDate.of(2023, 1, 1),
                            periodeTilDato = null,
                            delytelsesId = null,
                        ),
                    ),
                ),
            )
        }
        return hendelser
    }

    fun lagrePåløp() = persistenceService.påløpRepository.save(Påløp(kjøredato = LocalDateTime.now(), forPeriode = "2023-02"))
}
