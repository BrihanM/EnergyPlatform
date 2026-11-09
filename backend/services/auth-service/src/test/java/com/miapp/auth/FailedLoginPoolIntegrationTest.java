package com.miapp.auth;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.minimum-idle=0",
        "spring.datasource.hikari.connection-timeout=8000"
})
@AutoConfigureMockMvc
@ActiveProfiles("testcontainers")
@Testcontainers(disabledWithoutDocker = true)
class FailedLoginPoolIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired HikariDataSource dataSource;

    @Test
    void concurrentFailedLoginsPersistWithTwoConnectionsAndRecover() throws Exception {
        int attempts = 12;
        int before = jdbc.queryForObject(
                "SELECT count(*) FROM auth_audit_events WHERE event_type = 'LOGIN_FAILED'", Integer.class);
        var executor = Executors.newFixedThreadPool(attempts);
        var start = new CountDownLatch(1);
        try {
            var results = new ArrayList<java.util.concurrent.Future<Integer>>();
            for (int i = 0; i < attempts; i++) {
                results.add(executor.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                    return mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                                    .content("{\"identifier\":\"missing-user\",\"password\":\"WrongPassword123!\"}"))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (var result : results) assertThat(result.get(30, TimeUnit.SECONDS)).isEqualTo(401);
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM auth_audit_events WHERE event_type = 'LOGIN_FAILED'", Integer.class))
                    .isEqualTo(before + attempts);
            assertThat(dataSource.getMaximumPoolSize()).isEqualTo(2);
            assertThat(dataSource.getHikariPoolMXBean().getActiveConnections()).isZero();
            assertThat(jdbc.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
