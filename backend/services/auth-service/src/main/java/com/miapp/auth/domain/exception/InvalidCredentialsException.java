package com.miapp.auth.domain.exception;

/**
 * Error deliberadamente genérico para evitar enumerar cuentas o estados.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciales inválidas.");
    }
}
