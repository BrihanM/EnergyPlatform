package com.miapp.energy;

import com.jayway.jsonpath.JsonPath;
import com.miapp.energy.application.service.MeterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class MeterDeviceReadingIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6-alpine3.24");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        try (var connection = POSTGRES.createConnection("")) {
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS public.users (
                      id UUID PRIMARY KEY, first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL,
                      created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL)
                    """);
        } catch (java.sql.SQLException exception) {
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
    @Autowired MeterService service;
    @Autowired PlatformTransactionManager transactions;
    UUID owner;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM energy_market.energy_readings");
        jdbc.update("DELETE FROM energy_market.meter_devices");
        owner = UUID.randomUUID();
        jdbc.update("INSERT INTO public.users VALUES (?, 'Test', 'User', ?, ?)",
                owner, Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
    }

    record Device(UUID id, String credential) {}

    Device provision() throws Exception {
        String response = mvc.perform(post("/api/v1/market/devices")
                        .header("X-Authenticated-User-Id", owner).header("X-Authenticated-Roles", "ADMIN")
                        .contentType("application/json")
                        .content("{\"ownerUserId\":\"%s\",\"deviceIdentifier\":\"%s\"}"
                                .formatted(owner, UUID.randomUUID())))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        return new Device(UUID.fromString(JsonPath.read(response, "$.device.id")), JsonPath.read(response, "$.credential"));
    }

    ResultActions reading(UUID device, String credential, UUID reading, String generated, String surplus, String measured)
            throws Exception {
        var request = post("/api/v1/market/devices/{id}/readings", device)
                .contentType("application/json")
                .content("{\"id\":\"%s\",\"generatedEnergyKwh\":%s,\"surplusEnergyKwh\":%s,\"measuredAt\":\"%s\"}"
                        .formatted(reading, generated, surplus, measured));
        if (credential != null) request.header("X-Device-Credential", credential);
        return mvc.perform(request);
    }

    @Test
    void authenticatesPersistsClassifiesAndPreventsDuplicates() throws Exception {
        Device device = provision();
        UUID readingId = UUID.randomUUID();
        String measured = Instant.now().minusSeconds(10).toString();
        reading(device.id(), device.credential(), readingId, "2", "1", measured)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.validationStatus").value("VALID"))
                .andExpect(jsonPath("$.meterDeviceId").value(device.id().toString()));
        reading(device.id(), device.credential(), readingId, "2", "1", measured).andExpect(status().isConflict());
        reading(device.id(), device.credential(), UUID.randomUUID(), "2", "3", measured)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.validationStatus").value("INCONSISTENT"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM energy_market.energy_readings WHERE meter_device_id = ?",
                Integer.class, device.id())).isEqualTo(2);
        byte[] hash = jdbc.queryForObject("SELECT credential_hash FROM energy_market.meter_devices WHERE id = ?",
                byte[].class, device.id());
        assertThat(hash).hasSize(32).isNotEqualTo(device.credential().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void rejectsSpoofingUnknownDevicesAndUserRolesWithoutDeviceCredential() throws Exception {
        Device device = provision();
        String time = Instant.now().minusSeconds(1).toString();
        reading(device.id(), "incorrect", UUID.randomUUID(), "1", "1", time).andExpect(status().isUnauthorized());
        reading(UUID.randomUUID(), device.credential(), UUID.randomUUID(), "1", "1", time).andExpect(status().isUnauthorized());
        for (String role : new String[]{"NORMAL_USER", "ADMIN", "SECURITY_AUDITOR"}) {
            mvc.perform(post("/api/v1/market/devices/{id}/readings", device.id())
                            .header("Authorization", "Bearer user-token")
                            .header("X-Authenticated-User-Id", owner).header("X-Authenticated-Roles", role)
                            .contentType("application/json")
                            .content("{\"id\":\"%s\",\"generatedEnergyKwh\":1,\"surplusEnergyKwh\":1,\"measuredAt\":\"%s\"}"
                                    .formatted(UUID.randomUUID(), time)))
                    .andExpect(status().isUnauthorized());
        }
        Device other = provision();
        reading(other.id(), device.credential(), UUID.randomUUID(), "1", "1", time).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM energy_market.energy_readings", Integer.class)).isZero();
    }

    @Test
    void provisioningIsAdminOnlyAndRequiresAnExistingOwner() throws Exception {
        String body = "{\"ownerUserId\":\"%s\",\"deviceIdentifier\":\"meter-test\"}".formatted(owner);
        mvc.perform(post("/api/v1/market/devices").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
        for (String role : new String[]{"NORMAL_USER", "SECURITY_AUDITOR"}) {
            mvc.perform(post("/api/v1/market/devices").header("X-Authenticated-User-Id", owner)
                    .header("X-Authenticated-Roles", role).contentType("application/json").content(body))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/market/devices").header("X-Authenticated-User-Id", owner)
                        .header("X-Authenticated-Roles", "ADMIN").contentType("application/json")
                        .content("{\"ownerUserId\":\"%s\",\"deviceIdentifier\":\"missing-owner\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/market/devices").header("X-Authenticated-User-Id", owner)
                .header("X-Authenticated-Roles", "ADMIN").contentType("application/json").content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/market/devices").header("X-Authenticated-User-Id", owner)
                .header("X-Authenticated-Roles", "ADMIN").contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void rotatesSuspendsRevokesAndEnforcesAdmin() throws Exception {
        Device device = provision();
        String url = "/api/v1/market/devices/" + device.id();
        for (String role : new String[]{"NORMAL_USER", "SECURITY_AUDITOR"}) {
            mvc.perform(post(url + "/credential-rotation").header("X-Authenticated-User-Id", owner)
                    .header("X-Authenticated-Roles", role)).andExpect(status().isForbidden());
        }
        String rotated = mvc.perform(post(url + "/credential-rotation")
                        .header("X-Authenticated-User-Id", owner).header("X-Authenticated-Roles", "ADMIN"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        String replacement = JsonPath.read(rotated, "$.credential");
        assertThat(replacement).isNotEqualTo(device.credential());
        String time = Instant.now().minusSeconds(1).toString();
        reading(device.id(), device.credential(), UUID.randomUUID(), "1", "1", time).andExpect(status().isUnauthorized());
        reading(device.id(), replacement, UUID.randomUUID(), "1", "1", time).andExpect(status().isCreated());
        changeStatus(url, "SUSPENDED").andExpect(status().isOk());
        reading(device.id(), replacement, UUID.randomUUID(), "1", "1", time).andExpect(status().isUnauthorized());
        changeStatus(url, "ACTIVE").andExpect(status().isOk()).andExpect(jsonPath("$.credential").doesNotExist());
        reading(device.id(), replacement, UUID.randomUUID(), "1", "1", time).andExpect(status().isCreated());
        changeStatus(url, "REVOKED").andExpect(status().isOk());
        reading(device.id(), replacement, UUID.randomUUID(), "1", "1", time).andExpect(status().isUnauthorized());
        changeStatus(url, "ACTIVE").andExpect(status().isConflict());
        mvc.perform(post(url + "/credential-rotation").header("X-Authenticated-User-Id", owner)
                .header("X-Authenticated-Roles", "ADMIN")).andExpect(status().isConflict());
    }

    ResultActions changeStatus(String url, String state) throws Exception {
        return mvc.perform(patch(url + "/status").header("X-Authenticated-User-Id", owner)
                .header("X-Authenticated-Roles", "ADMIN").contentType("application/json")
                .content("{\"status\":\"%s\"}".formatted(state)));
    }

    @Test
    void rejectsInvalidMeasurementsAndEnforcesDatabaseConstraints() throws Exception {
        Device device = provision();
        String time = Instant.now().minusSeconds(1).toString();
        reading(device.id(), device.credential(), UUID.randomUUID(), "-1", "0", time).andExpect(status().isBadRequest());
        reading(device.id(), device.credential(), UUID.randomUUID(), "null", "0", time).andExpect(status().isBadRequest());
        reading(device.id(), device.credential(), UUID.randomUUID(), "1", "0", "not-a-date").andExpect(status().isBadRequest());
        reading(device.id(), device.credential(), UUID.randomUUID(), "1", "0", Instant.now().plusSeconds(60).toString())
                .andExpect(status().isBadRequest());
        reading(device.id(), device.credential(), UUID.randomUUID(), "1.0000001", "0", time).andExpect(status().isBadRequest());
        reading(device.id(), device.credential(), UUID.randomUUID(), "1e2147483647", "0", time).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/market/devices/{id}/readings", device.id())
                        .header("X-Device-Credential", device.credential()).contentType("application/json")
                        .content("{\"id\":\"%s\",\"meterDeviceId\":\"%s\",\"generatedEnergyKwh\":1,\"surplusEnergyKwh\":0,\"measuredAt\":\"%s\"}"
                                .formatted(UUID.randomUUID(), UUID.randomUUID(), time)))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM energy_market.energy_readings", Integer.class)).isZero();
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO energy_market.energy_readings VALUES (?, ?, 1, 2, now(), now(), 'VALID')
                """, UUID.randomUUID(), device.id())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO energy_market.energy_readings VALUES (?, ?, 1, 1, now(), now(), 'VALID')
                """, UUID.randomUUID(), UUID.randomUUID())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rollsBackReadingOnTechnicalFailure() throws Exception {
        Device device = provision();
        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            service.ingest(device.id(), device.credential(), UUID.randomUUID(),
                    BigDecimal.ONE, BigDecimal.ONE, Instant.now().minusSeconds(1));
            throw new IllegalStateException("simulated failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM energy_market.energy_readings", Integer.class)).isZero();
    }
}
