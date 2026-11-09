package com.miapp.auth.application.port.out;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;
import com.miapp.auth.domain.model.Role;

/**
 * Puerto para emitir access tokens sin acoplar la aplicación a JWT.
 */
public interface AccessTokenGeneratorPort {

    AccessToken generate(UUID subject, UUID sessionId, String username, Set<Role> roles, Instant issuedAt);

    record AccessToken(String value, long expiresInSeconds) {
    }
}
