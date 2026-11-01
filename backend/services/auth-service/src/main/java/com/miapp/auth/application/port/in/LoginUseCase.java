package com.miapp.auth.application.port.in;

import com.miapp.auth.application.command.LoginCommand;
import com.miapp.auth.application.result.LoginResult;

/**
 * Puerto de entrada para validar credenciales de una cuenta.
 */
public interface LoginUseCase {

    /**
     * Valida el identificador y la contraseña sin emitir tokens ni crear sesión.
     *
     * @param command credenciales recibidas
     * @return información segura del usuario autenticado
     */
    LoginResult login(LoginCommand command);
}
