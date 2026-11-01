package com.miapp.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Locale;

/**
 * Configuración externalizada de la cookie de refresh token.
 */
@ConfigurationProperties(prefix = "security.refresh-token.cookie")
public record RefreshTokenCookieProperties(String name, boolean secure, String sameSite,
                                           String path, String domain, String maxAge) {

    public RefreshTokenCookieProperties {
        if (name == null || name.isBlank() || path == null || path.isBlank()
                || sameSite == null || sameSite.isBlank() || maxAge == null
                || maxAge.isBlank()) {
            throw new IllegalArgumentException("Invalid refresh token cookie configuration");
        }
        sameSite = sameSite.substring(0, 1).toUpperCase(Locale.ROOT)
                + sameSite.substring(1).toLowerCase(Locale.ROOT);
        if ("None".equals(sameSite) && !secure) {
            throw new IllegalArgumentException("SameSite=None requires Secure=true");
        }
    }

    public Duration duration() {
        try {
            long milliseconds = Long.parseLong(maxAge);
            if (milliseconds < 0) {
                throw new IllegalArgumentException("Cookie max-age must not be negative");
            }
            return Duration.ofMillis(milliseconds);
        } catch (NumberFormatException ignored) {
            Duration duration = Duration.parse(maxAge);
            if (duration.isNegative()) {
                throw new IllegalArgumentException("Cookie max-age must not be negative");
            }
            return duration;
        }
    }
}
