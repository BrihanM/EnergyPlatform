package com.miapp.auth.infrastructure.adapter.in.web;

import com.miapp.auth.application.result.LoginResult;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO HTTP del login; no contiene credenciales ni tokens.
 */
public record LoginResponse(
        boolean authenticated,
        UserResponse user) {

    static LoginResponse from(LoginResult result) {
        LoginResult.UserSummary user = result.user();
        return new LoginResponse(result.authenticated(), new UserResponse(
                user.id(), user.firstName(), user.lastName(), user.username(),
                user.email(), user.createdAt()));
    }

    /**
     * Información segura del usuario autenticado.
     */
    public record UserResponse(
            UUID id,
            String firstName,
            String lastName,
            String username,
            String email,
            Instant createdAt) {
    }
}
