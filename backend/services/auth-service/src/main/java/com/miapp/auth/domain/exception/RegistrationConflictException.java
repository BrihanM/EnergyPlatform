package com.miapp.auth.domain.exception;

/**
 * Indica que una restricción de integridad impidió completar el registro.
 *
 * <p>Se utiliza para carreras concurrentes cuando la base de datos rechaza la
 * escritura después de las comprobaciones optimistas de existencia.</p>
 */
public class RegistrationConflictException extends RuntimeException {

    public RegistrationConflictException() {
        super("The registration conflicts with an existing account");
    }
}
