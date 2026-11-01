package com.miapp.auth.application.port.out;

/**
 * Puerto técnico para generar el valor opaco de un refresh token.
 */
public interface RefreshTokenGeneratorPort {

    String generate();
}
