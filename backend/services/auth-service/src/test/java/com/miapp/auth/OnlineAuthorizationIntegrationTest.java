package com.miapp.auth;

import com.jayway.jsonpath.JsonPath;
import com.miapp.auth.application.service.UserAdministrationService;
import com.miapp.auth.application.service.RefreshTokenHistoryCleanup;
import com.miapp.auth.domain.exception.InvalidRoleOperationException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("testcontainers")
@Testcontainers(disabledWithoutDocker = true)
class OnlineAuthorizationIntegrationTest extends PostgresIntegrationTest {
    private static final String CALLER_SECRET = "test-introspection-secret-at-least-32-characters";
    private static final String JWT_SECRET = "test-jwt-secret-that-is-at-least-32-bytes-long";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAdministrationService administration;
    @Autowired PlatformTransactionManager transactions;
    @Autowired RefreshTokenHistoryCleanup historyCleanup;
    private final List<UUID> createdUsers = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (UUID userId : createdUsers) {
            jdbc.update("DELETE FROM sessions WHERE account_id IN (SELECT id FROM accounts WHERE user_id = ?)", userId);
            jdbc.update("DELETE FROM user_roles WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM accounts WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    record Login(UUID userId, String accessToken, Cookie refresh) {}

    private Login login() throws Exception {
        String username = "u" + UUID.randomUUID().toString().replace("-", "");
        String registration = mvc.perform(post("/api/v1/auth/register").contentType("application/json")
                        .content(("{\"firstName\":\"Test\",\"lastName\":\"User\",\"username\":\"%s\","
                                + "\"email\":\"%s@example.com\",\"password\":\"Password123!\"}")
                                .formatted(username, username)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID userId = UUID.fromString(JsonPath.read(registration, "$.id"));
        createdUsers.add(userId);
        var result = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"identifier\":\"%s\",\"password\":\"Password123!\"}".formatted(username)))
                .andExpect(status().isOk()).andReturn().getResponse();
        return new Login(userId, JsonPath.read(result.getContentAsString(), "$.accessToken"),
                result.getCookie("refresh_token"));
    }

    private org.springframework.test.web.servlet.ResultActions introspect(String token) throws Exception {
        return mvc.perform(post("/internal/authorization/introspect")
                .header("X-Internal-Auth-Secret", CALLER_SECRET).header("Authorization", "Bearer " + token));
    }

    private String signed(UUID userId, String sid) {
        var builder = Jwts.builder().issuer("test-issuer").subject(userId.toString())
                .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(120)))
                .claim("username", "test").claim("roles", java.util.List.of("NORMAL_USER"));
        if (sid != null) builder.claim("sid", sid);
        return builder.signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    private String sid(String token) {
        return Jwts.parser().verifyWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token).getPayload().get("sid", String.class);
    }

    @Test
    void logoutRevokesAccessAndRefreshForTheSamePersistedSession() throws Exception {
        Login login = login();
        assertThat(login.refresh()).isNotNull();
        introspect(login.accessToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(login.userId().toString()))
                .andExpect(jsonPath("$.roles[0]").value("NORMAL_USER"));
        mvc.perform(post("/api/v1/auth/logout").cookie(login.refresh())).andExpect(status().isNoContent());
        introspect(login.accessToken()).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(login.refresh())).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshKeepsSessionBindingAndRotatesTheRefreshToken() throws Exception {
        Login login = login();
        var response = mvc.perform(post("/api/v1/auth/refresh").cookie(login.refresh()))
                .andExpect(status().isOk()).andReturn().getResponse();
        String refreshed = JsonPath.read(response.getContentAsString(), "$.accessToken");
        Cookie rotated = response.getCookie("refresh_token");
        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(login.refresh().getValue());
        assertThat(sid(refreshed)).isEqualTo(sid(login.accessToken()));
        mvc.perform(post("/api/v1/auth/refresh").cookie(login.refresh())).andExpect(status().isUnauthorized());
        introspect(refreshed).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout").cookie(rotated)).andExpect(status().isNoContent());
        introspect(refreshed).andExpect(status().isUnauthorized());
        introspect(login.accessToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithAnOldCookieRevokesTheSessionAfterMultipleRotations() throws Exception {
        Login login = login();
        var first = mvc.perform(post("/api/v1/auth/refresh").cookie(login.refresh()))
                .andExpect(status().isOk()).andReturn().getResponse();
        var second = mvc.perform(post("/api/v1/auth/refresh").cookie(first.getCookie("refresh_token")))
                .andExpect(status().isOk()).andReturn().getResponse();
        mvc.perform(post("/api/v1/auth/refresh").cookie(login.refresh())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout").cookie(login.refresh())).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/logout").cookie(login.refresh())).andExpect(status().isNoContent());
        introspect(login.accessToken()).andExpect(status().isUnauthorized());
        introspect(JsonPath.read(second.getContentAsString(), "$.accessToken")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(second.getCookie("refresh_token")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("refresh_token", "unknown")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredSessionHistoryIsRemovedWithoutAffectingActiveSessions() throws Exception {
        Login expired = login();
        Login active = login();
        mvc.perform(post("/api/v1/auth/refresh").cookie(expired.refresh())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/refresh").cookie(active.refresh())).andExpect(status().isOk());
        UUID expiredId = UUID.fromString(sid(expired.accessToken()));
        UUID activeId = UUID.fromString(sid(active.accessToken()));
        jdbc.update("UPDATE sessions SET expires_at = created_at WHERE id = ?", expiredId);
        historyCleanup.cleanup();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rotated_refresh_token_history WHERE session_id = ?",
                Integer.class, expiredId)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rotated_refresh_token_history WHERE session_id = ?",
                Integer.class, activeId)).isEqualTo(1);
        mvc.perform(post("/api/v1/auth/logout").cookie(expired.refresh())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout").cookie(active.refresh())).andExpect(status().isNoContent());
    }

    @org.junit.jupiter.api.RepeatedTest(5)
    void concurrentLogoutAndRefreshNeverLeaveAnActiveSession() throws Exception {
        Login login = login();
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var refresh = executor.submit(() -> {
                start.await();
                return mvc.perform(post("/api/v1/auth/refresh").cookie(login.refresh()))
                        .andReturn().getResponse().getStatus();
            });
            var logout = executor.submit(() -> {
                start.await();
                return mvc.perform(post("/api/v1/auth/logout").cookie(login.refresh()))
                        .andReturn().getResponse().getStatus();
            });
            start.countDown();
            assertThat(refresh.get(15, TimeUnit.SECONDS)).isIn(200, 401);
            assertThat(logout.get(15, TimeUnit.SECONDS)).isEqualTo(204);
            introspect(login.accessToken()).andExpect(status().isUnauthorized());
            assertThat(jdbc.queryForObject("SELECT revoked_at IS NOT NULL FROM sessions WHERE id = ?",
                    Boolean.class, UUID.fromString(sid(login.accessToken())))).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentRefreshAllowsOnlyOneRotation() throws Exception {
        Login login = login();
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Integer> refresh = () -> {
                start.await();
                return mvc.perform(post("/api/v1/auth/refresh").cookie(login.refresh()))
                        .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(refresh);
            var second = executor.submit(refresh);
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 401);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void simultaneousLogoutsAreIdempotentAndDoNotRevokeAnotherSession() throws Exception {
        Login target = login();
        Login other = login();
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Integer> logout = () -> {
                start.await();
                return mvc.perform(post("/api/v1/auth/logout").cookie(target.refresh()))
                        .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(logout);
            var second = executor.submit(logout);
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactly(204, 204);
            introspect(target.accessToken()).andExpect(status().isUnauthorized());
            introspect(other.accessToken()).andExpect(status().isOk());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void preexistingCombinedRolesAreDeniedWithoutChangingAssignments() throws Exception {
        Login user = login();
        jdbc.update("INSERT INTO user_roles (user_id, role_code, assigned_at) VALUES (?, 'ADMIN', now())",
                user.userId());
        jdbc.update("INSERT INTO user_roles (user_id, role_code, assigned_at) VALUES (?, 'SECURITY_AUDITOR', now())",
                user.userId());
        introspect(user.accessToken()).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM user_roles WHERE user_id = ?
                AND role_code IN ('ADMIN', 'SECURITY_AUDITOR')
                """, Integer.class, user.userId())).isEqualTo(2);
    }

    @Test
    void concurrentRefreshAndSuspensionLeaveNoActiveSession() throws Exception {
        Login admin = login();
        Login target = login();
        jdbc.update("INSERT INTO user_roles (user_id, role_code, assigned_at) VALUES (?, 'ADMIN', now())",
                admin.userId());
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var refresh = executor.submit(() -> {
                start.await();
                return mvc.perform(post("/api/v1/auth/refresh").cookie(target.refresh()))
                    .andReturn().getResponse().getStatus();
            });
            var suspend = executor.submit(() -> {
                start.await();
                administration.suspend(admin.userId(), target.userId());
                return true;
            });
            start.countDown();
            assertThat(refresh.get(15, TimeUnit.SECONDS)).isIn(200, 401);
            assertThat(suspend.get(15, TimeUnit.SECONDS)).isTrue();
            introspect(target.accessToken()).andExpect(status().isUnauthorized());
            assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM sessions s JOIN accounts a ON a.id = s.account_id
                WHERE a.user_id = ? AND s.revoked_at IS NULL
                """, Integer.class, target.userId())).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentLoginAndSuspensionLeaveNoActiveSession() throws Exception {
        Login admin = login();
        Login target = login();
        jdbc.update("INSERT INTO user_roles (user_id, role_code, assigned_at) VALUES (?, 'ADMIN', now())",
                admin.userId());
        String username = jdbc.queryForObject("SELECT username FROM accounts WHERE user_id = ?",
                String.class, target.userId());
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var newLogin = executor.submit(() -> {
                start.await();
                return mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                            .content("{\"identifier\":\"%s\",\"password\":\"Password123!\"}".formatted(username)))
                    .andReturn().getResponse().getStatus();
            });
            var suspend = executor.submit(() -> {
                start.await();
                administration.suspend(admin.userId(), target.userId());
                return true;
            });
            start.countDown();
            assertThat(newLogin.get(15, TimeUnit.SECONDS)).isIn(200, 401);
            assertThat(suspend.get(15, TimeUnit.SECONDS)).isTrue();
            introspect(target.accessToken()).andExpect(status().isUnauthorized());
            assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM sessions s JOIN accounts a ON a.id = s.account_id
                WHERE a.user_id = ? AND s.revoked_at IS NULL
                """, Integer.class, target.userId())).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentReactivationAndLoginDoNotRestoreOldSessions() throws Exception {
        Login admin = login();
        Login target = login();
        jdbc.update("INSERT INTO user_roles (user_id, role_code, assigned_at) VALUES (?, 'ADMIN', now())",
                admin.userId());
        administration.suspend(admin.userId(), target.userId());
        String username = jdbc.queryForObject("SELECT username FROM accounts WHERE user_id = ?",
                String.class, target.userId());
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var newLogin = executor.submit(() -> {
                start.await();
                return mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                                .content("{\"identifier\":\"%s\",\"password\":\"Password123!\"}".formatted(username)))
                        .andReturn().getResponse().getStatus();
            });
            var reactivation = executor.submit(() -> {
                start.await();
                administration.reactivate(admin.userId(), target.userId());
                return true;
            });
            start.countDown();
            assertThat(newLogin.get(15, TimeUnit.SECONDS)).isIn(200, 401);
            assertThat(reactivation.get(15, TimeUnit.SECONDS)).isTrue();
            introspect(target.accessToken()).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/v1/auth/refresh").cookie(target.refresh())).andExpect(status().isUnauthorized());
            assertThat(jdbc.queryForObject("SELECT status FROM accounts WHERE user_id = ?",
                    String.class, target.userId())).isEqualTo("ACTIVE");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void twoAdministratorsCannotBothSuspendTheSameAccount() throws Exception {
        Login first = login();
        Login second = login();
        Login target = login();
        for (Login admin : List.of(first, second)) {
            jdbc.update("INSERT INTO user_roles (user_id, role_code, assigned_at) VALUES (?, 'ADMIN', now())",
                    admin.userId());
        }
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var one = executor.submit(() -> suspendConcurrently(first.userId(), target.userId(), start));
            var two = executor.submit(() -> suspendConcurrently(second.userId(), target.userId(), start));
            start.countDown();
            assertThat(List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM auth_audit_events
                    WHERE affected_user_id = ? AND event_type = 'USER_SUSPENDED'
                    """, Integer.class, target.userId())).isEqualTo(1);
            introspect(target.accessToken()).andExpect(status().isUnauthorized());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void currentAccountAndRolesAreReadOnEveryRequest() throws Exception {
        Login login = login();
        introspect(login.accessToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("NORMAL_USER"));
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", login.userId());
        jdbc.update("INSERT INTO user_roles (user_id, role_code, assigned_at) VALUES (?, 'SECURITY_AUDITOR', now())",
                login.userId());
        introspect(login.accessToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("SECURITY_AUDITOR"));
        jdbc.update("UPDATE accounts SET status = 'LOCKED' WHERE user_id = ?", login.userId());
        introspect(login.accessToken()).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(login.refresh())).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongCallerMissingAndMismatchedSessionClaims() throws Exception {
        Login first = login();
        Login second = login();
        mvc.perform(post("/internal/authorization/introspect")
                        .header("X-Internal-Auth-Secret", "invalid")
                        .header("Authorization", "Bearer " + first.accessToken()))
                .andExpect(status().isForbidden());
        introspect(signed(first.userId(), null)).andExpect(status().isUnauthorized());
        introspect(signed(first.userId(), UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
        introspect(signed(first.userId(), sid(second.accessToken()))).andExpect(status().isUnauthorized());
        introspect(signed(UUID.randomUUID(), sid(first.accessToken()))).andExpect(status().isUnauthorized());
    }

    @Test
    void suspensionRevokesSessionsPermanentlyAndAuditorCanInspectTheEvents() throws Exception {
        Login admin = login();
        Login target = login();
        Login auditor = login();
        jdbc.update("INSERT INTO user_roles (user_id, role_code, assigned_at) VALUES (?, 'ADMIN', now())",
                admin.userId());
        jdbc.update("INSERT INTO user_roles (user_id, role_code, assigned_at) VALUES (?, 'SECURITY_AUDITOR', now())",
                auditor.userId());
        String username = jdbc.queryForObject("SELECT username FROM accounts WHERE user_id = ?",
                String.class, target.userId());

        mvc.perform(get("/api/v1/admin/users").param("page", "0").param("size", "10")
                        .header("X-Authenticated-User-Id", admin.userId())
                        .header("X-Authenticated-Roles", "ADMIN"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/users").param("page", "0").param("size", "10")
                        .header("X-Authenticated-User-Id", auditor.userId())
                        .header("X-Authenticated-Roles", "SECURITY_AUDITOR"))
                .andExpect(status().isForbidden());
        introspect(target.accessToken()).andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/users/{id}/suspension", target.userId())
                        .header("X-Authenticated-User-Id", admin.userId())
                        .header("X-Authenticated-Roles", "ADMIN"))
                .andExpect(status().isNoContent());
        introspect(target.accessToken()).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(target.refresh())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"identifier\":\"%s\",\"password\":\"Password123!\"}".formatted(username)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/users/{id}/reactivation", target.userId())
                        .header("X-Authenticated-User-Id", admin.userId())
                        .header("X-Authenticated-Roles", "ADMIN"))
                .andExpect(status().isNoContent());
        introspect(target.accessToken()).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(target.refresh())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"identifier\":\"%s\",\"password\":\"Password123!\"}".formatted(username)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/auth/audit/events").param("page", "0").param("size", "10")
                        .param("user", target.userId().toString()).param("type", "USER_SUSPENDED")
                        .header("X-Authenticated-User-Id", auditor.userId())
                        .header("X-Authenticated-Roles", "SECURITY_AUDITOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].actorId").value(admin.userId().toString()))
                .andExpect(jsonPath("$.items[0].affectedUserId").value(target.userId().toString()));
        mvc.perform(get("/api/v1/auth/audit/events").param("page", "0").param("size", "10")
                        .header("X-Authenticated-User-Id", admin.userId())
                        .header("X-Authenticated-Roles", "ADMIN"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/users/{id}/suspension", admin.userId())
                        .header("X-Authenticated-User-Id", admin.userId())
                        .header("X-Authenticated-Roles", "ADMIN"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT status FROM accounts WHERE user_id = ?",
                String.class, admin.userId())).isEqualTo("ACTIVE");
    }

    @Test
    void suspensionRollbackRestoresAccountSessionsAndAudit() throws Exception {
        Login admin = login();
        Login target = login();
        jdbc.update("INSERT INTO user_roles (user_id, role_code, assigned_at) VALUES (?, 'ADMIN', now())",
                admin.userId());
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    administration.suspend(admin.userId(), target.userId());
                    throw new IllegalStateException("force rollback");
                })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM accounts WHERE user_id = ?",
                String.class, target.userId())).isEqualTo("ACTIVE");
        introspect(target.accessToken()).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM auth_audit_events
                WHERE affected_user_id = ? AND event_type = 'USER_SUSPENDED'
                """, Integer.class, target.userId())).isZero();
    }

    @Test
    void auditWriteFailureRollsBackSuspensionAndSessionRevocation() throws Exception {
        Login admin = login();
        Login target = login();
        jdbc.update("INSERT INTO user_roles (user_id, role_code, assigned_at) VALUES (?, 'ADMIN', now())",
                admin.userId());
        jdbc.execute("""
                ALTER TABLE auth_audit_events ADD CONSTRAINT test_reject_suspension
                CHECK (event_type <> 'USER_SUSPENDED') NOT VALID
                """);
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    administration.suspend(admin.userId(), target.userId()))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(jdbc.queryForObject("SELECT status FROM accounts WHERE user_id = ?",
                    String.class, target.userId())).isEqualTo("ACTIVE");
            introspect(target.accessToken()).andExpect(status().isOk());
        } finally {
            jdbc.execute("ALTER TABLE auth_audit_events DROP CONSTRAINT test_reject_suspension");
        }
    }

    @Test
    void concurrentAdministratorsCannotSuspendEachOther() throws Exception {
        Login first = login();
        Login second = login();
        for (Login admin : List.of(first, second)) {
            jdbc.update("INSERT INTO user_roles (user_id, role_code, assigned_at) VALUES (?, 'ADMIN', now())",
                    admin.userId());
        }
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var one = executor.submit(() -> suspendConcurrently(first.userId(), second.userId(), start));
            var two = executor.submit(() -> suspendConcurrently(second.userId(), first.userId(), start));
            start.countDown();
            assertThat(List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM accounts WHERE user_id IN (?, ?) AND status = 'ACTIVE'
                    """, Integer.class, first.userId(), second.userId())).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean suspendConcurrently(UUID actor, UUID target, CountDownLatch start) throws InterruptedException {
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for start");
        try {
            administration.suspend(actor, target);
            return true;
        } catch (InvalidRoleOperationException exception) {
            return false;
        }
    }
}
