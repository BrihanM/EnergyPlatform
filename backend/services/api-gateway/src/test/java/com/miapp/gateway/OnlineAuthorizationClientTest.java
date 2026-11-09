package com.miapp.gateway;

import com.miapp.gateway.security.OnlineAuthorizationClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class OnlineAuthorizationClientTest {
    private static final String SECRET = "test-introspection-secret-at-least-32-characters";
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> body = new AtomicReference<>();
    private HttpServer server;
    private OnlineAuthorizationClient client;
    private String userId;

    @BeforeEach
    void start() throws Exception {
        userId = UUID.randomUUID().toString();
        body.set("{\"userId\":\"" + userId + "\",\"username\":\"user\",\"roles\":[\"ADMIN\"]}");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/authorization/introspect", exchange -> {
            if (!SECRET.equals(exchange.getRequestHeaders().getFirst("X-Internal-Auth-Secret"))) {
                exchange.sendResponseHeaders(403, -1);
                exchange.close();
                return;
            }
            String value = body.get();
            if ("delay".equals(value)) {
                try { Thread.sleep(1000); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
                value = "{}";
            }
            byte[] data = value.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            try {
                exchange.sendResponseHeaders(status.get(), data.length);
                try (var output = exchange.getResponseBody()) { output.write(data); }
            } catch (java.io.IOException exception) {
                exchange.close();
            }
        });
        server.start();
        client = new OnlineAuthorizationClient("http://127.0.0.1:" + server.getAddress().getPort(),
                SECRET, Duration.ofSeconds(5));
    }

    @AfterEach
    void stop() { if (server != null) server.stop(0); }

    @Test
    void returnsOnlyCurrentRolesAndRejectsMismatchedUser() {
        assertThat(client.validate("token", userId).block().roles()).containsExactly("ADMIN");
        assertThatThrownBy(() -> client.validate("token", UUID.randomUUID().toString()).block())
                .isInstanceOf(OnlineAuthorizationClient.AuthorizationUnavailableException.class);
    }

    @Test
    void unauthorizedStateIsDistinctFromServiceErrorsAndInvalidResponse() {
        status.set(401);
        assertThatThrownBy(() -> client.validate("token", userId).block())
                .isInstanceOf(OnlineAuthorizationClient.InvalidAuthorizationException.class);
        status.set(500);
        assertThatThrownBy(() -> client.validate("token", userId).block())
                .isInstanceOf(OnlineAuthorizationClient.AuthorizationUnavailableException.class);
        status.set(200);
        body.set("{\"userId\":\"" + userId + "\",\"username\":\"user\",\"roles\":[\"UNKNOWN\"]}");
        assertThatThrownBy(() -> client.validate("token", userId).block())
                .isInstanceOf(OnlineAuthorizationClient.AuthorizationUnavailableException.class);
        body.set("{not json");
        assertThatThrownBy(() -> client.validate("token", userId).block())
                .isInstanceOf(OnlineAuthorizationClient.AuthorizationUnavailableException.class);
    }

    @Test
    void timeoutAndConnectionFailureNeverAuthorize() {
        client = new OnlineAuthorizationClient("http://127.0.0.1:" + server.getAddress().getPort(),
                SECRET, Duration.ofMillis(200));
        body.set("delay");
        assertThatThrownBy(() -> client.validate("token", userId).block())
                .isInstanceOf(OnlineAuthorizationClient.AuthorizationUnavailableException.class);
        server.stop(0);
        server = null;
        assertThatThrownBy(() -> client.validate("token", userId).block())
                .isInstanceOf(OnlineAuthorizationClient.AuthorizationUnavailableException.class);
    }
}
