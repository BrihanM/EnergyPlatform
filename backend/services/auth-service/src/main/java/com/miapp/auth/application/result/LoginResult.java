package com.miapp.auth.application.result;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado seguro del login, sin credenciales ni tokens.
 */
public record LoginResult(
        boolean authenticated,
        UserSummary user) {

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
