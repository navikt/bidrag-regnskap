package no.nav.bidrag.regnskap.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@Configuration
@EnableAspectJAutoProxy
@ConfigurationPropertiesScan
@SecurityScheme(bearerFormat = "JWT", name = "bearer-key", scheme = "bearer", type = SecuritySchemeType.HTTP)
@OpenAPIDefinition(info = Info(title = "bidrag-regnskap", version = "v1"), security = [SecurityRequirement(name = "bearer-key")])
@ComponentScan(basePackages = ["no.nav.bidrag.commons.util", "no.nav.bidrag.commons.security.maskinporten"])
class BidragRegnskapConfiguration {

    @Bean
    fun lockProvider(dataSource: DataSource): LockProvider {
        return JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder().withJdbcTemplate(JdbcTemplate(dataSource)).usingDbTime().build(),
        )
    }
}
