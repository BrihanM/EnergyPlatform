package com.miapp.auth.application.port.out;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;
import com.miapp.auth.domain.model.Role;

/**
 * Puerto para emitir access tokens sin acoplar la aplicación a JWT.
 */
public interface AccessTokenGeneratorPort {

    AccessToken generate(UUID subject, String username, Set<Role> roles, Instant issuedAt);

    default AccessToken generate(UUID subject, String username, Instant issuedAt) {
        return generate(subject, username, Set.of(), issuedAt);
    }

    record AccessToken(String value, long expiresInSeconds) {
    }
}
