package com.miapp.auth.application.port.out;

/**
 * Puerto técnico para transformar y validar refresh tokens sin persistirlos.
 */
public interface RefreshTokenHashingPort {

    String hash(String refreshToken);

    boolean matches(String refreshToken, String refreshTokenHash);
}
