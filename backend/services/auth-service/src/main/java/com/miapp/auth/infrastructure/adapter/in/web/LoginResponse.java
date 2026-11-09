package com.miapp.auth.infrastructure.adapter.in.web;

import com.miapp.auth.application.result.LoginResult;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;
import com.miapp.auth.domain.model.Role;

/**
 * DTO HTTP del login; no contiene credenciales ni tokens.
 */
public record LoginResponse(
        boolean authenticated,
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user,
        Set<Role> roles) {

    static LoginResponse from(LoginResult result) {
        LoginResult.UserSummary user = result.user();
        return new LoginResponse(result.authenticated(), result.accessToken(), "Bearer",
                result.expiresIn(), new UserResponse(
                user.id(), user.firstName(), user.lastName(), user.username(),
                user.email(), user.createdAt()), result.roles());
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
