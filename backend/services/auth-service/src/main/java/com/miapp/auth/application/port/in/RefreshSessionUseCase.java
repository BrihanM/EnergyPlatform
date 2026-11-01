package com.miapp.auth.application.port.in;

import com.miapp.auth.application.result.AuthenticationResult;

/**
 * Puerto de entrada para validar y rotar una sesión mediante cookie.
 */
public interface RefreshSessionUseCase {

    AuthenticationResult refresh(String refreshToken);
}
