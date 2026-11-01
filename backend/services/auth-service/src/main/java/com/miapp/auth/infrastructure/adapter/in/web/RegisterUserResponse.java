package com.miapp.auth.infrastructure.adapter.in.web;

import com.miapp.auth.application.result.RegisterUserResult;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO HTTP seguro del registro; no contiene credenciales ni hashes.
 */
public record RegisterUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String username,
        String email,
        String status,
        Instant createdAt) {

    static RegisterUserResponse from(RegisterUserResult result) {
        return new RegisterUserResponse(result.id(), result.firstName(), result.lastName(),
                result.username(), result.email(), result.status().name(), result.createdAt());
    }
}
