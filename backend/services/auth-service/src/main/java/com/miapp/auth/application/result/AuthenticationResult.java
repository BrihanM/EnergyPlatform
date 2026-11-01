package com.miapp.auth.application.result;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado interno de autenticación. El refresh token sólo se entrega al
 * adaptador HTTP para construir una cookie HttpOnly.
 */
public record AuthenticationResult(
        String accessToken,
        long expiresIn,
        String refreshToken,
        UserSummary user) {

    public record UserSummary(UUID id, String firstName, String lastName,
                              String username, String email, Instant createdAt) {
    }

    @Override
    public String toString() {
        return "AuthenticationResult{accessToken=[REDACTED], refreshToken=[REDACTED], user="
                + user.username() + "}";
    }
}
