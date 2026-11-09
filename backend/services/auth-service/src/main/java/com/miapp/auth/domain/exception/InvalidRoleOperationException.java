package com.miapp.auth.domain.exception;

/**
 * Indicates an unauthorized or invalid role-management operation.
 */
public class InvalidRoleOperationException extends RuntimeException {

    public InvalidRoleOperationException(String message) {
        super(message);
    }
}
