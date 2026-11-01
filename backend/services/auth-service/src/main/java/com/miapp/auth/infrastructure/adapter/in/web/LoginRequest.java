package com.miapp.auth.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO HTTP de entrada para validar credenciales.
 */
public record LoginRequest(
        @NotBlank String identifier,
        @NotBlank String password) {
}
