package com.miapp.auth.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Propiedades de duración de sesiones y refresh tokens.
 */
@ConfigurationProperties(prefix = "security.refresh-token")
public record SessionProperties(String expiration) {

    public SessionProperties {
        if (expiration == null || expiration.isBlank()) {
            throw new IllegalArgumentException("security.refresh-token.expiration must be positive");
        }
    }

    public Duration duration() {
        try {
            long milliseconds = Long.parseLong(expiration);
            if (milliseconds <= 0) {
                throw new IllegalArgumentException("security.refresh-token.expiration must be positive");
            }
            return Duration.ofMillis(milliseconds);
        } catch (NumberFormatException ignored) {
            Duration duration = Duration.parse(expiration);
            if (duration.isZero() || duration.isNegative()) {
                throw new IllegalArgumentException("security.refresh-token.expiration must be positive");
            }
            return duration;
        }
    }
}
