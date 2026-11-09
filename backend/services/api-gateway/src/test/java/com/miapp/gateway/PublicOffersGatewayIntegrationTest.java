package com.miapp.gateway;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PublicOffersGatewayIntegrationTest {
    private static final String SECRET = "test-gateway-secret-that-is-at-least-32-bytes";
    private static final String OFFER_ID = "2e0c531b-59c4-48b3-8532-34a1ed105831";
    private static HttpServer upstream;

    @LocalServerPort int port;

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry registry) throws IOException {
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.createContext("/", exchange -> {
            String user = exchange.getRequestHeaders().getFirst("X-Authenticated-User-Id");
            String roles = exchange.getRequestHeaders().getFirst("X-Authenticated-Roles");
            String account = exchange.getRequestHeaders().getFirst("X-Authenticated-Account-Id");
            String username = exchange.getRequestHeaders().getFirst("X-Authenticated-Username");
            byte[] body = ("%s|%s|%s|%s".formatted(user, roles, account, username)).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        upstream.start();
        String uri = "http://127.0.0.1:" + upstream.getAddress().getPort();
        String[] paths = { "/api/v1/auth/**", "/api/v1/market/**",
                "/api/v1/offers/**", "/api/v1/transactions/**" };
        for (int i = 0; i < paths.length; i++) {
            String prefix = "spring.cloud.gateway.server.webflux.routes[" + i + "].";
            String id = "test-route-" + i;
            String path = paths[i];
            registry.add(prefix + "id", () -> id);
            registry.add(prefix + "uri", () -> uri);
            registry.add(prefix + "predicates[0]", () -> "Path=" + path);
        }
        registry.add("spring.cloud.gateway.server.webflux.globalcors.add-to-simple-url-handler-mapping", () -> "true");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowed-origins", () -> "http://localhost:5173");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowed-methods", () -> "GET,POST,OPTIONS");
        registry.add("spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowed-headers", () -> "Authorization,Content-Type,Idempotency-Key");
    }

    @AfterAll
    static void stopUpstream() {
        if (upstream != null) upstream.stop(0);
    }

    static Stream<String> publicPaths() {
        return Stream.of("/api/v1/offers", "/api/v1/offers/" + OFFER_ID,
                "/api/v1/market/offers", "/api/v1/market/offers/" + OFFER_ID);
    }

    @ParameterizedTest
    @MethodSource("publicPaths")
    void publicOfferReadsStripForgedIdentity(String path) {
        client().get().uri(path)
                .header("X-Authenticated-User-Id", UUID.randomUUID().toString())
                .header("X-Authenticated-Roles", "ADMIN")
                .header("X-Authenticated-Account-Id", "spoofed")
                .header("X-Authenticated-Username", "spoofed")
                .exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("null|null|null|null");
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> protectedPaths() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.GET, "/api/v1/offers/me"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.GET, "/api/v1/market/offers/me"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.GET, "/api/v1/offers/admin"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.GET, "/api/v1/admin/roles"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.POST, "/api/v1/market/devices"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.POST, "/api/v1/market/devices/" + OFFER_ID + "/credential-rotation"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.PATCH, "/api/v1/market/devices/" + OFFER_ID + "/status"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.GET, "/api/v1/market/devices/" + OFFER_ID + "/readings"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.POST, "/api/v1/market/devices/not-a-uuid/readings"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.GET, "/api/v1/offers/" + OFFER_ID + "/private"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.HEAD, "/api/v1/offers"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.POST, "/api/v1/offers"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.POST, "/api/v1/market/offers"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.PUT, "/api/v1/offers/" + OFFER_ID),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.DELETE, "/api/v1/offers/" + OFFER_ID),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.POST, "/api/v1/transactions"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.GET, "/api/v1/transactions/me"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.GET, "/api/v1/transactions/" + OFFER_ID));
    }

    @Test
    void deviceIngestionForwardsWithoutUserJwtAndStripsForgedIdentity() {
        client().post().uri("/api/v1/market/devices/" + OFFER_ID + "/readings")
                .header("X-Device-Credential", "test-credential")
                .header("X-Authenticated-User-Id", UUID.randomUUID().toString())
                .header("X-Authenticated-Roles", "ADMIN")
                .exchange().expectStatus().isOk()
                .expectBody(String.class).isEqualTo("null|null|null|null");
    }

    @ParameterizedTest
    @MethodSource("protectedPaths")
    void protectedPathsRejectForgedIdentityWithoutToken(HttpMethod method, String path) {
        client().method(method).uri(path)
                .header("X-Authenticated-User-Id", UUID.randomUUID().toString())
                .header("X-Authenticated-Roles", "NORMAL_USER")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void protectedRequestsUseTokenIdentityNotClientHeaders() {
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder().subject(userId.toString()).issuer("test-issuer")
                .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(300)))
                .claim("username", "test-user").claim("roles", java.util.List.of("NORMAL_USER"))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
        client().post().uri("/api/v1/transactions")
                .header("Authorization", "Bearer " + token)
                .header("X-Authenticated-User-Id", UUID.randomUUID().toString())
                .header("X-Authenticated-Roles", "ADMIN")
                .header("X-Authenticated-Account-Id", "spoofed")
                .header("X-Authenticated-Username", "spoofed")
                .exchange().expectStatus().isOk()
                .expectBody(String.class).isEqualTo(userId + "|NORMAL_USER|null|test-user");
    }

    @Test
    void corsPreflightAllowsIdempotencyKey() {
        var response = client().options().uri("/api/v1/transactions")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type,idempotency-key")
                .exchange().expectStatus().isOk().returnResult(Void.class);
        assertThat(response.getResponseHeaders().getAccessControlAllowHeaders())
                .anyMatch(header -> header.equalsIgnoreCase("idempotency-key"));
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://127.0.0.1:" + port).build();
    }
}
