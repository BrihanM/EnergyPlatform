package com.miapp.auth.application.result;

import com.miapp.auth.domain.model.AccountStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado seguro del registro, sin contraseña ni hash.
 */
public record RegisterUserResult(
        UUID id,
        String firstName,
        String lastName,
        String username,
        String email,
        AccountStatus status,
        Instant createdAt) {
}
