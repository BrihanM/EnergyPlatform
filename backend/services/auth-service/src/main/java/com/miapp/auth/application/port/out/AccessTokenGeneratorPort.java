package com.miapp.auth.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Puerto para emitir access tokens sin acoplar la aplicación a JWT.
 */
public interface AccessTokenGeneratorPort {

    AccessToken generate(UUID subject, String username, Instant issuedAt);

    record AccessToken(String value, long expiresInSeconds) {
    }
}
