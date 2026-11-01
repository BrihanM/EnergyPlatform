package com.miapp.auth.domain.exception;

/**
 * Indica que el correo ya está asociado a una cuenta.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("The email is already registered");
    }
}
