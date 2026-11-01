package com.miapp.auth.domain.exception;

/**
 * Indica que los datos incumplen reglas de negocio del registro.
 */
public class InvalidRegistrationException extends RuntimeException {

    public InvalidRegistrationException(String message) {
        super(message);
    }
}
