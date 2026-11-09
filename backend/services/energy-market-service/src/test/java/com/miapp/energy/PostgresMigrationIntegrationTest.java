package com.miapp.energy;

import com.miapp.energy.application.service.OfferQuery;
import com.miapp.energy.domain.model.EnergyOffer;
import com.miapp.energy.domain.model.EnergyOfferStatus;
import com.miapp.energy.infrastructure.adapter.out.postgresql.JdbcEnergyOfferRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
        POSTGRES.start();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS public.users (
                        id UUID PRIMARY KEY,
                        first_name VARCHAR(100) NOT NULL,
                        last_name VARCHAR(100) NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL,
                        updated_at TIMESTAMPTZ NOT NULL
                    )
                    """);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not prepare Auth user table for migration test",
                    exception);
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.schemas", () -> "energy_market");
        registry.add("spring.flyway.default-schema", () -> "energy_market");
        registry.add("spring.flyway.create-schemas", () -> true);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcEnergyOfferRepository repository;

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
                """, String.class)).isEqualTo("5");
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'energy_market'
                  and table_name not like 'flyway_schema_history'
                """, Integer.class)).isEqualTo(7);
    }

    @Test
    void persistsQueriesUpdatesAndCancelsOfferWithOwnershipFilters() {
        UUID seller = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO public.users (id, first_name, last_name, created_at, updated_at)
                VALUES (?, 'Seller', 'Test', ?, ?)
                """, seller, Timestamp.from(now), Timestamp.from(now));

        EnergyOffer offer = EnergyOffer.create(seller, new BigDecimal("15.000"),
                new BigDecimal("500.00"), now.plusSeconds(3600), now);
        repository.save(offer);

        assertThat(repository.findAvailable(OfferQuery.available(0, 20, "createdAt",
                null, null, null, null, null, null)).content())
                .extracting(EnergyOffer::id).containsExactly(offer.id());
        assertThat(repository.findBySeller(seller, new OfferQuery(null, null, null, null,
                null, null, null, 0, 20, "createdAt")).content())
                .extracting(EnergyOffer::sellerId).containsOnly(seller);

        offer.update(new BigDecimal("16.000"), new BigDecimal("501.00"),
                now.plusSeconds(7200), now.plusSeconds(1));
        EnergyOffer updated = repository.update(offer);
        assertThat(updated.energyAmountKwh()).isEqualByComparingTo("16.000");

        updated.cancel(now.plusSeconds(2));
        EnergyOffer cancelled = repository.update(updated);
        assertThat(cancelled.status()).isEqualTo(EnergyOfferStatus.CANCELLED);
        assertThat(repository.findAvailable(OfferQuery.available(0, 20, "createdAt",
                null, null, null, null, null, null)).content()).isEmpty();
    }
}
