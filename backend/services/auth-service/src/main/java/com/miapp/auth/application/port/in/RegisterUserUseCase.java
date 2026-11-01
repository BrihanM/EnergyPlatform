package com.miapp.auth.application.port.in;

import com.miapp.auth.application.command.RegisterUserCommand;
import com.miapp.auth.application.result.RegisterUserResult;

/**
 * Puerto de entrada para registrar una identidad y su cuenta de acceso.
 */
public interface RegisterUserUseCase {

    /**
     * Ejecuta el registro atómico de una identidad y una cuenta.
     *
     * @param command datos de registro sin acoplamiento al transporte HTTP
     * @return datos seguros de la cuenta creada
     */
    RegisterUserResult register(RegisterUserCommand command);
}
