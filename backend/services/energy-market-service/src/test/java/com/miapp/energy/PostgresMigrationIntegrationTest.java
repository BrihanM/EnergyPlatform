package com.miapp.energy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the service owns an isolated schema and Flyway history.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18.6-alpine3.24");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.schemas", () -> "energy_market");
        registry.add("spring.flyway.default-schema", () -> "energy_market");
        registry.add("spring.flyway.create-schemas", () -> true);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsOnlyEnergyMarketSchemaAndOwnMigrationHistory() {
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from information_schema.schemata
                where schema_name = 'energy_market'
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from energy_market.flyway_schema_history
                """, Integer.class)).isPositive();
        assertThat(jdbcTemplate.queryForObject("""
                select version from energy_market.flyway_schema_history
                where success = true
                order by installed_rank desc
                limit 1
                """, String.class)).isEqualTo("1");
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'energy_market'
                  and table_name not like 'flyway_schema_history'
                """, Integer.class)).isZero();
    }
}
