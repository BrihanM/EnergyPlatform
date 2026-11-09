package com.miapp.gateway;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.miapp.gateway.security.GatewayJwtProperties;
import com.miapp.gateway.security.GatewayJwtValidator;
import com.miapp.gateway.security.GatewaySecurityConfiguration;
import com.miapp.gateway.security.OnlineAuthorizationClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import reactor.core.publisher.Mono;

/**
 * Verifica la sanitización de headers y la propagación de identidad validada.
 */
class GatewayAuthenticationFilterTest {

    private static final String SECRET = "test-gateway-secret-that-is-at-least-32-bytes";

    @Test
    void removesClientIdentityHeadersBeforeAddingTrustedValues() {
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder()
                .subject(userId.toString())
                .issuer("mi-app-auth")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .claim("sid", UUID.randomUUID().toString())
                .claim("username", "trusted-user")
                .claim("roles", java.util.List.of("NORMAL_USER"))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
        var request = MockServerHttpRequest.get("/api/v1/protected")
                .header("Authorization", "Bearer " + token)
                .header("X-Authenticated-User-Id", "malicious-value")
                .header("X-Authenticated-Username", "malicious-user")
                .header("X-Authenticated-Roles", "ADMIN")
                .header("X-Internal-Auth-Secret", "forged-secret")
                .build();
        var exchange = MockServerWebExchange.from(request);
        AtomicReference<String> propagatedUser = new AtomicReference<>();
        AtomicReference<String> propagatedUsername = new AtomicReference<>();
        AtomicReference<String> propagatedRoles = new AtomicReference<>();
        OnlineAuthorizationClient online = mock(OnlineAuthorizationClient.class);
        when(online.validate(token, userId.toString())).thenReturn(Mono.just(
                new OnlineAuthorizationClient.CurrentIdentity(userId.toString(), "trusted-user", java.util.Set.of("SECURITY_AUDITOR"))));
        WebFilter filter = new GatewaySecurityConfiguration().gatewayAuthenticationFilter(
                new GatewayJwtValidator(new GatewayJwtProperties("mi-app-auth", SECRET)),
                JsonMapper.builder().addModule(new JavaTimeModule()).build(), online);

        filter.filter(exchange, next -> {
            propagatedUser.set(nextRequestUser(next));
            propagatedUsername.set(next.getRequest().getHeaders()
                    .getFirst("X-Authenticated-Username"));
            propagatedRoles.set(next.getRequest().getHeaders()
                    .getFirst("X-Authenticated-Roles"));
            assertThat(next.getRequest().getHeaders().getFirst("X-Internal-Auth-Secret")).isNull();
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertThat(propagatedUser).hasValue(userId.toString());
        assertThat(propagatedUsername).hasValue("trusted-user");
        assertThat(propagatedRoles).hasValue("SECURITY_AUDITOR");
    }

    @Test
    void revokedSessionAndUnavailableAuthNeverReachUpstream() {
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder().subject(userId.toString()).issuer("mi-app-auth")
                .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(60)))
                .claim("sid", UUID.randomUUID().toString()).claim("username", "user")
                .claim("roles", java.util.List.of("NORMAL_USER"))
                .signWith(key(), Jwts.SIG.HS256).compact();
        OnlineAuthorizationClient online = mock(OnlineAuthorizationClient.class);
        WebFilter filter = new GatewaySecurityConfiguration().gatewayAuthenticationFilter(
                new GatewayJwtValidator(new GatewayJwtProperties("mi-app-auth", SECRET)),
                JsonMapper.builder().addModule(new JavaTimeModule()).build(), online);
        when(online.validate(token, userId.toString()))
                .thenReturn(Mono.error(new OnlineAuthorizationClient.InvalidAuthorizationException()))
                .thenReturn(Mono.error(new OnlineAuthorizationClient.AuthorizationUnavailableException()));
        for (HttpStatus expected : java.util.List.of(HttpStatus.UNAUTHORIZED, HttpStatus.SERVICE_UNAVAILABLE)) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/protected")
                    .header("Authorization", "Bearer " + token).build());
            filter.filter(exchange, next -> { throw new AssertionError("Request reached upstream"); }).block();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(expected);
        }
    }

    private String nextRequestUser(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst("X-Authenticated-User-Id");
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
