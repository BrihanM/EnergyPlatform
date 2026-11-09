package com.miapp.gateway;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.miapp.gateway.security.GatewayJwtProperties;
import com.miapp.gateway.security.GatewayJwtValidator;
import com.miapp.gateway.security.GatewaySecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
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
                .claim("username", "trusted-user")
                .claim("roles", java.util.List.of("NORMAL_USER"))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
        var request = MockServerHttpRequest.get("/api/v1/protected")
                .header("Authorization", "Bearer " + token)
                .header("X-Authenticated-User-Id", "malicious-value")
                .header("X-Authenticated-Username", "malicious-user")
                .header("X-Authenticated-Roles", "ADMIN")
                .build();
        var exchange = MockServerWebExchange.from(request);
        AtomicReference<String> propagatedUser = new AtomicReference<>();
        AtomicReference<String> propagatedUsername = new AtomicReference<>();
        AtomicReference<String> propagatedRoles = new AtomicReference<>();
        WebFilter filter = new GatewaySecurityConfiguration().gatewayAuthenticationFilter(
                new GatewayJwtValidator(new GatewayJwtProperties("mi-app-auth", SECRET)),
                JsonMapper.builder().build());

        filter.filter(exchange, next -> {
            propagatedUser.set(nextRequestUser(next));
            propagatedUsername.set(next.getRequest().getHeaders()
                    .getFirst("X-Authenticated-Username"));
            propagatedRoles.set(next.getRequest().getHeaders()
                    .getFirst("X-Authenticated-Roles"));
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertThat(propagatedUser).hasValue(userId.toString());
        assertThat(propagatedUsername).hasValue("trusted-user");
        assertThat(propagatedRoles).hasValue("NORMAL_USER");
    }

    private String nextRequestUser(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst("X-Authenticated-User-Id");
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
