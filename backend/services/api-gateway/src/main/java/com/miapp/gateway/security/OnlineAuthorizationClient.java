package com.miapp.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/** Queries Auth for current account, session and role state without caching. */
@Component
public class OnlineAuthorizationClient {
    private static final Set<String> ALLOWED_ROLES = Set.of("NORMAL_USER", "ADMIN", "SECURITY_AUDITOR");
    private final WebClient client;
    private final String secret;
    private final Duration timeout;

    public OnlineAuthorizationClient(@Value("${security.introspection.uri}") String uri,
                                     @Value("${security.introspection.secret}") String secret,
                                     @Value("${security.introspection.timeout:2s}") Duration timeout) {
        if (secret == null || secret.length() < 32 || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Invalid introspection configuration");
        }
        this.client = WebClient.builder().baseUrl(uri).build();
        this.secret = secret;
        this.timeout = timeout;
    }

    public Mono<CurrentIdentity> validate(String token, String expectedUserId) {
        return client.post().uri("/internal/authorization/introspect")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Internal-Auth-Secret", secret)
                .exchangeToMono(response -> {
                    if (response.statusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                        return response.releaseBody().then(Mono.error(new InvalidAuthorizationException()));
                    }
                    if (!response.statusCode().is2xxSuccessful()) {
                        return response.releaseBody().then(Mono.error(new AuthorizationUnavailableException()));
                    }
                    return response.bodyToMono(CurrentIdentity.class)
                            .switchIfEmpty(Mono.error(new AuthorizationUnavailableException()));
                })
                .timeout(timeout)
                .map(identity -> validateResponse(identity, expectedUserId))
                .onErrorMap(error -> !(error instanceof InvalidAuthorizationException)
                                && !(error instanceof AuthorizationUnavailableException),
                        AuthorizationUnavailableException::new);
    }

    private CurrentIdentity validateResponse(CurrentIdentity identity, String expectedUserId) {
        try {
            UUID.fromString(identity.userId());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AuthorizationUnavailableException(exception);
        }
        if (!identity.userId().equals(expectedUserId) || identity.username() == null
                || identity.username().isBlank() || identity.roles() == null
                || identity.roles().stream().anyMatch(role -> role == null || !ALLOWED_ROLES.contains(role))) {
            throw new AuthorizationUnavailableException();
        }
        return identity;
    }

    public record CurrentIdentity(String userId, String username, Set<String> roles) {}
    public static final class InvalidAuthorizationException extends RuntimeException {}
    public static final class AuthorizationUnavailableException extends RuntimeException {
        public AuthorizationUnavailableException() {}
        public AuthorizationUnavailableException(Throwable cause) { super(cause); }
    }
}
