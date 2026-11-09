package com.miapp.auth;

import com.jayway.jsonpath.JsonPath;
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
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
}
