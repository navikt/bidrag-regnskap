package no.nav.bidrag.regnskap.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.regnskap.BidragRegnskapLocal
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.entity.Påløp
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeAll
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

@Disabled("Denne er for å teste hastighet ved påløpskjøring. Bør ikke kjøres fast.")
@Transactional
@ActiveProfiles("test")
@EnableMockOAuth2Server
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [BidragRegnskapLocal::class])
@EmbeddedKafka(partitions = 1, brokerProperties = ["listeners=PLAINTEXT://localhost:9092", "port=9092"])
class PåløpskjøringServiceIT {

    companion object {

        val antallOppdrag = 2000

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
    private lateinit var påløpskjøringService: PåløpskjøringService

    @Autowired
    private lateinit var persistenceService: PersistenceService

    private var påløp: Påløp? = null

    @BeforeAll
    fun beforeAll() {
        påløp = lagrePåløp()
        opprettOppdragOgOppdragsperioder(antallOppdrag)
    }

    @Test
    fun `skal ta tiden på opprettelse av manglende konteringer`() {
        val tidsbruk = measureTime {
            påløpskjøringService.startPåløpskjøring(påløp!!, schedulertKjøring = false, genererFil = false)
        }

        val tidsbrukISekunder = tidsbruk.inWholeMilliseconds.toDouble() / 1000
        println("Det tok $tidsbrukISekunder sekunder å opprette $antallOppdrag oppdrag.")

        val oppdrag = persistenceService.oppdragRepository.findAll()
        for (i in 0..<antallOppdrag) {
            val konteringer = oppdrag[i].oppdragsperioder.first().konteringer
            konteringer shouldHaveSize 2
            konteringer.first().overføringstidspunkt shouldNotBe null
            konteringer.first().behandlingsstatusOkTidspunkt shouldNotBe null
            konteringer.last().overføringstidspunkt shouldNotBe null
            konteringer.last().behandlingsstatusOkTidspunkt shouldNotBe null
        }

        val utsattOppdragsperiode = oppdrag[antallOppdrag].oppdragsperioder.first()
        utsattOppdragsperiode.konteringer shouldHaveSize 2
        utsattOppdragsperiode.konteringer[0].sendtIPåløpsperiode shouldBe null
        utsattOppdragsperiode.konteringer[0].overføringstidspunkt shouldBe null
        utsattOppdragsperiode.konteringer[0].behandlingsstatusOkTidspunkt shouldBe null
        utsattOppdragsperiode.konteringer[1].sendtIPåløpsperiode shouldBe null
        utsattOppdragsperiode.konteringer[1].overføringstidspunkt shouldBe null
        utsattOppdragsperiode.konteringer[1].behandlingsstatusOkTidspunkt shouldBe null

        val feiledOppdragsperiode = oppdrag[antallOppdrag + 1].oppdragsperioder.first()
        feiledOppdragsperiode.konteringer shouldHaveSize 2
        feiledOppdragsperiode.konteringer[0].sendtIPåløpsperiode shouldBe null
        feiledOppdragsperiode.konteringer[0].overføringstidspunkt shouldNotBe null
        feiledOppdragsperiode.konteringer[0].behandlingsstatusOkTidspunkt shouldBe null
        feiledOppdragsperiode.konteringer[1].sendtIPåløpsperiode shouldBe null
        feiledOppdragsperiode.konteringer[1].overføringstidspunkt shouldBe null
        feiledOppdragsperiode.konteringer[1].behandlingsstatusOkTidspunkt shouldBe null
    }

    fun lagrePåløp() = persistenceService.påløpRepository.save(Påløp(kjøredato = LocalDateTime.now(), forPeriode = "2023-02"))

    @Suppress("SameParameterValue")
    private fun opprettOppdragOgOppdragsperioder(antallOppdrag: Int) {
        for (i in 0..<antallOppdrag) {
            val oppdrag = Oppdrag(
                stønadType = "BIDRAG",
                sakId = Random.nextInt().toString(),
                skyldnerIdent = PersonidentGenerator.genererFødselsnummer(),
                gjelderIdent = PersonidentGenerator.genererFødselsnummer(),
                mottakerIdent = PersonidentGenerator.genererFødselsnummer(),
            )
            val oppdragsperiode = Oppdragsperiode(
                oppdrag = oppdrag,
                vedtakId = Random.nextInt(),
                vedtakType = "FASTSETTELSE",
                beløp = BigDecimal(Random.nextDouble()),
                valuta = "NOK",
                periodeFra = LocalDate.of(2023, 1, 1),
                vedtaksdato = LocalDate.now(),
                opprettetAv = "TEST",
                periodeTil = null,
                delytelseId = null,
                referanse = null,
            )
            oppdrag.oppdragsperioder = listOf(oppdragsperiode)

            persistenceService.oppdragRepository.save(oppdrag)
        }

        val utsattOppdrag = Oppdrag(
            stønadType = "BIDRAG",
            sakId = Random.nextInt().toString(),
            utsattTilDato = LocalDate.now().plusDays(1),
            skyldnerIdent = PersonidentGenerator.genererFødselsnummer(),
            gjelderIdent = PersonidentGenerator.genererFødselsnummer(),
            mottakerIdent = PersonidentGenerator.genererFødselsnummer(),
        )
        val utsattOppdragsperiode = Oppdragsperiode(
            oppdrag = utsattOppdrag,
            vedtakId = Random.nextInt(),
            vedtakType = "FASTSETTELSE",
            beløp = BigDecimal(Random.nextDouble()),
            valuta = "NOK",
            periodeFra = LocalDate.of(2023, 1, 1),
            vedtaksdato = LocalDate.now(),
            opprettetAv = "TEST",
            periodeTil = null,
            delytelseId = null,
            referanse = null,
        )
        utsattOppdrag.oppdragsperioder = listOf(utsattOppdragsperiode)
        persistenceService.oppdragRepository.save(utsattOppdrag)

        val feiletOppdrag = Oppdrag(
            stønadType = "BIDRAG",
            sakId = Random.nextInt().toString(),
            skyldnerIdent = PersonidentGenerator.genererFødselsnummer(),
            gjelderIdent = PersonidentGenerator.genererFødselsnummer(),
            mottakerIdent = PersonidentGenerator.genererFødselsnummer(),
        )
        val feiledVedtakId = Random.nextInt()
        val feiletOppdragsperiode = Oppdragsperiode(
            oppdrag = feiletOppdrag,
            vedtakId = feiledVedtakId,
            vedtakType = "FASTSETTELSE",
            beløp = BigDecimal(Random.nextDouble()),
            valuta = "NOK",
            periodeFra = LocalDate.of(2023, 1, 1),
            vedtaksdato = LocalDate.now(),
            opprettetAv = "TEST",
            periodeTil = null,
            delytelseId = null,
            referanse = null,
        )
        val feiletKontering = Kontering(
            oppdragsperiode = feiletOppdragsperiode,
            overføringsperiode = "2023-01",
            søknadType = "FASTSETTELSE",
            transaksjonskode = "B1",
            type = "NY",
            vedtakId = feiledVedtakId,
            behandlingsstatusOkTidspunkt = null,
            overføringstidspunkt = LocalDateTime.now().minusDays(1),
        )

        feiletOppdragsperiode.konteringer = listOf(feiletKontering)
        feiletOppdrag.oppdragsperioder = listOf(feiletOppdragsperiode)
        persistenceService.oppdragRepository.save(feiletOppdrag)
    }
}
