package com.miapp.auth.application.result;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;
import com.miapp.auth.domain.model.Role;

/**
 * Resultado interno de autenticación. El refresh token sólo se entrega al
 * adaptador HTTP para construir una cookie HttpOnly.
 */
public record AuthenticationResult(
        String accessToken,
        long expiresIn,
        String refreshToken,
        UserSummary user,
        Set<Role> roles) {

    public AuthenticationResult(String accessToken, long expiresIn, String refreshToken,
                                UserSummary user) {
        this(accessToken, expiresIn, refreshToken, user, Set.of());
    }

    public record UserSummary(UUID id, String firstName, String lastName,
                              String username, String email, Instant createdAt) {
    }

    @Override
    public String toString() {
        return "AuthenticationResult{accessToken=[REDACTED], refreshToken=[REDACTED], user="
                + user.username() + "}";
    }
}
