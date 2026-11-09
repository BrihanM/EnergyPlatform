package com.miapp.auth.application.result;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;
import com.miapp.auth.domain.model.Role;

/**
 * Resultado seguro del login, sin credenciales ni tokens.
 */
public record LoginResult(
        boolean authenticated,
        UserSummary user,
        String accessToken,
        long expiresIn,
        String refreshToken,
        Set<Role> roles) {

    public LoginResult(boolean authenticated, UserSummary user, String accessToken,
                       long expiresIn, String refreshToken) {
        this(authenticated, user, accessToken, expiresIn, refreshToken, Set.of());
    }

    public LoginResult(boolean authenticated, UserSummary user) {
        this(authenticated, user, null, 0, null, Set.of());
    }

    @Override
    public String toString() {
        return "LoginResult{authenticated=" + authenticated
                + ", accessToken=[REDACTED], refreshToken=[REDACTED], user=" + user.username() + "}";
    }

    /**
     * Datos públicos mínimos del usuario autenticado.
     */
    public record UserSummary(
            UUID id,
            String firstName,
            String lastName,
            String username,
            String email,
            Instant createdAt) {
    }
}
