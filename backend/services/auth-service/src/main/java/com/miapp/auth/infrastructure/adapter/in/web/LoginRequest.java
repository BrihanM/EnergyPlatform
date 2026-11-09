package com.miapp.auth.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO HTTP de entrada para validar credenciales.
 *
 * <p>Los límites coinciden con el registro (email máx. 320, contraseña máx. 128) para evitar
 * payloads desproporcionados sin imponer reglas de complejidad a cuentas existentes.</p>
 */
public record LoginRequest(
        @NotBlank @Size(max = 320) String identifier,
        @NotBlank @Size(max = 128) String password) {
}
