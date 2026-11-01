package com.miapp.auth.application.port.in;

/**
 * Puerto de entrada para revocar la sesión asociada a una cookie.
 */
public interface LogoutUseCase {

    void logout(String refreshToken);
}
