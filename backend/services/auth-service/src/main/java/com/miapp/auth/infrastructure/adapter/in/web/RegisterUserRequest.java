package com.miapp.auth.infrastructure.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO HTTP de entrada para el registro.
 */
public record RegisterUserRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(min = 3, max = 50)
        @jakarta.validation.constraints.Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{1,48}[A-Za-z0-9]")
        String username,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 12, max = 128) String password) {
}
