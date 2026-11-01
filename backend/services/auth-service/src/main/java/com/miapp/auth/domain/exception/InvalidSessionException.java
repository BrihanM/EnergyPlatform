package com.miapp.auth.domain.exception;

/**
 * Error genérico para sesiones inexistentes, expiradas, revocadas o inválidas.
 */
public class InvalidSessionException extends RuntimeException {

    public InvalidSessionException() {
        super("Sesión inválida o expirada.");
    }
}
