package com.miapp.auth.domain.exception;

/**
 * Indica que el nombre de usuario ya está asociado a una cuenta.
 */
public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException() {
        super("The username is already registered");
    }
}
