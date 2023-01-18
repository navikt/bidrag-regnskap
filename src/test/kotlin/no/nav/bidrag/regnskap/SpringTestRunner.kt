package no.nav.bidrag.regnskap

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.bidrag.regnskap.maskinporten.MaskinportenWireMock
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

@ActiveProfiles("test")
@EnableMockOAuth2Server
@SpringBootTest(classes = [BidragRegnskapLocal::class])
class SpringTestRunner {

  companion object {
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
  private lateinit var applicationContext: ApplicationContext

  @AfterEach
  fun reset() {
    resetWiremockServers()
  }

  private fun resetWiremockServers() {
    applicationContext.getBeansOfType(WireMockServer::class.java)
      .values
      .forEach(WireMockServer::resetRequests)
  }
}