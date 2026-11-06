package com.miapp.energy;

import com.miapp.energy.application.service.EnergyTransactionService;
import com.miapp.energy.domain.exception.EnergyOfferDomainException;
import com.miapp.energy.domain.model.EnergyOffer;
import com.miapp.energy.infrastructure.adapter.out.postgresql.JdbcEnergyOfferRepository;
import com.miapp.energy.infrastructure.adapter.out.postgresql.JdbcEnergyTransactionRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class EnergyTransactionIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18.6-alpine3.24");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        try (var connection = POSTGRES.createConnection("")) {
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS public.users (
                        id UUID PRIMARY KEY,
                        first_name VARCHAR(100) NOT NULL,
                        last_name VARCHAR(100) NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL,
                        updated_at TIMESTAMPTZ NOT NULL
                    )
                    """);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not prepare Auth user table", exception);
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.schemas", () -> "energy_market");
        registry.add("spring.flyway.default-schema", () -> "energy_market");
        registry.add("spring.flyway.create-schemas", () -> true);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JdbcEnergyOfferRepository offers;
    @Autowired JdbcEnergyTransactionRepository transactions;
    @Autowired EnergyTransactionService service;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM energy_market.operator_verifications");
        jdbc.update("DELETE FROM energy_market.transaction_idempotency_keys");
        jdbc.update("DELETE FROM energy_market.energy_transactions");
        jdbc.update("DELETE FROM energy_market.energy_offers");
        jdbc.update("DELETE FROM public.users");
    }

    @Test
    void controllerExecutesAutomaticPurchaseAndReturnsVerificationSnapshot() throws Exception {
        UUID seller = user("Seller");
        UUID buyer = user("Buyer");
        UUID offerId = offer(seller, Instant.now().plusSeconds(3600));

        mvc.perform(post("/api/v1/transactions")
                        .header("X-Authenticated-User-Id", buyer)
                        .header("X-Authenticated-Roles", "NORMAL_USER")
                        .header("Idempotency-Key", "purchase-1")
                        .contentType("application/json")
                        .content("""
                                {"offerId":"%s"}
                                """.formatted(offerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.verificationResult").value("ACCEPTED"))
                .andExpect(jsonPath("$.currency").value("COP"))
                .andExpect(jsonPath("$.totalAmount").value(7500.00));

        assertThat(count("energy_market.energy_transactions")).isEqualTo(1);
        assertThat(count("energy_market.operator_verifications")).isEqualTo(1);
        assertThat(offerStatus(offerId)).isEqualTo("SOLD");
    }

    @Test
    void replayReturnsOriginalTransactionAndDoesNotCreateAnotherVerification() throws Exception {
        UUID seller = user("Seller");
        UUID buyer = user("Buyer");
        UUID offerId = offer(seller, Instant.now().plusSeconds(3600));
        String body = "{\"offerId\":\"%s\"}".formatted(offerId);

        String first = mvc.perform(post("/api/v1/transactions")
                        .header("X-Authenticated-User-Id", buyer)
                        .header("X-Authenticated-Roles", "NORMAL_USER")
                        .header("Idempotency-Key", "replay-1")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = mvc.perform(post("/api/v1/transactions")
                        .header("X-Authenticated-User-Id", buyer)
                        .header("X-Authenticated-Roles", "NORMAL_USER")
                        .header("Idempotency-Key", "replay-1")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(jsonId(second)).isEqualTo(jsonId(first));
        assertThat(count("energy_market.energy_transactions")).isEqualTo(1);
        assertThat(count("energy_market.operator_verifications")).isEqualTo(1);
    }

    @Test
    void concurrentDifferentBuyersCanConfirmOnlyOnePurchase() throws Exception {
        UUID seller = user("Seller");
        UUID buyerOne = user("Buyer One");
        UUID buyerTwo = user("Buyer Two");
        UUID offerId = offer(seller, Instant.now().plusSeconds(3600));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Object> purchaseOne = () -> concurrentPurchase(buyerOne, offerId, "buyer-one", ready, start);
        Callable<Object> purchaseTwo = () -> concurrentPurchase(buyerTwo, offerId, "buyer-two", ready, start);
        Future<Object> first = executor.submit(purchaseOne);
        Future<Object> second = executor.submit(purchaseTwo);
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        List<Object> results = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        executor.shutdownNow();

        assertThat(results).filteredOn(EnergyOfferDomainException.class::isInstance)
                .hasSize(1);
        assertThat(count("energy_market.energy_transactions")).isEqualTo(1);
        assertThat(countWhere("energy_market.energy_transactions", "status = 'CONFIRMED'")).isEqualTo(1);
        assertThat(count("energy_market.operator_verifications")).isEqualTo(1);
        assertThat(offerStatus(offerId)).isEqualTo("SOLD");
    }

    @Test
    void concurrentSameKeyDoesNotDuplicateThePurchase() throws Exception {
        UUID seller = user("Seller");
        UUID buyer = user("Buyer");
        UUID offerId = offer(seller, Instant.now().plusSeconds(3600));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Object> task = () -> concurrentPurchase(buyer, offerId, "same-key", ready, start);
        Future<Object> first = executor.submit(task);
        Future<Object> second = executor.submit(task);
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        Object firstResult = first.get(20, TimeUnit.SECONDS);
        Object secondResult = second.get(20, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertThat(List.of(firstResult, secondResult)).filteredOn(EnergyOfferDomainException.class::isInstance)
                .hasSize(1);
        assertThat(count("energy_market.energy_transactions")).isEqualTo(1);
        assertThat(count("energy_market.operator_verifications")).isEqualTo(1);
    }

    @Test
    void technicalSimulatorFailureRollsBackReservationTransactionAndIdempotency() {
        UUID seller = user("Seller");
        UUID buyer = user("Buyer");
        UUID offerId = offer(seller, Instant.now().plusSeconds(3600));
        EnergyTransactionService failingService = new EnergyTransactionService(offers, transactions,
                ignored -> { throw new IllegalStateException("simulator unavailable"); });
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> template.execute(status -> {
            failingService.purchase(buyer, offerId, "technical-failure");
            return null;
        })).isInstanceOf(IllegalStateException.class);

        assertThat(offerStatus(offerId)).isEqualTo("ACTIVE");
        assertThat(count("energy_market.energy_transactions")).isZero();
        assertThat(count("energy_market.operator_verifications")).isZero();
        assertThat(count("energy_market.transaction_idempotency_keys")).isZero();
    }

    @Test
    void controllerRejectsMissingIdentityRoleAndIdempotencyHeader() throws Exception {
        UUID offerId = UUID.randomUUID();
        mvc.perform(post("/api/v1/transactions")
                        .contentType("application/json")
                        .content("{\"offerId\":\"%s\"}".formatted(offerId)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/transactions")
                        .header("X-Authenticated-User-Id", UUID.randomUUID())
                        .header("X-Authenticated-Roles", "ADMIN")
                        .contentType("application/json")
                        .content("{\"offerId\":\"%s\"}".formatted(offerId)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/transactions")
                        .header("X-Authenticated-User-Id", UUID.randomUUID())
                        .header("X-Authenticated-Roles", "NORMAL_USER")
                        .contentType("application/json")
                        .content("{\"offerId\":\"%s\"}".formatted(offerId)))
                .andExpect(status().isBadRequest());
    }

    private Object concurrentPurchase(UUID buyer, UUID offerId, String key,
                                      CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
            return service.purchase(buyer, offerId, key);
        } catch (Exception exception) {
            return exception;
        }
    }

    private UUID user(String name) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO public.users (id, first_name, last_name, created_at, updated_at)
                VALUES (?, ?, 'Test', ?, ?)
                """, id, name, Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    private UUID offer(UUID seller, Instant expiresAt) {
        EnergyOffer offer = EnergyOffer.create(seller, new BigDecimal("15.000"),
                new BigDecimal("500.00"), expiresAt, Instant.now());
        offers.save(offer);
        return offer.id();
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private long countWhere(String table, String condition) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + condition, Long.class);
    }

    private String offerStatus(UUID offerId) {
        return jdbc.queryForObject("SELECT status FROM energy_market.energy_offers WHERE id = ?",
                String.class, offerId);
    }

    private String jsonId(String json) {
        return json.replaceFirst(".*\"id\":\"([^\"]+)\".*", "$1");
    }
}
