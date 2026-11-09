package com.miapp.auth.application.command;

/**
 * Entrada del caso de uso de registro. La contraseña sólo vive durante la
 * ejecución y su representación textual está deliberadamente redactada,
 * igual que los datos personales, para que no aparezcan en logs.
 */
public record RegisterUserCommand(
        String firstName,
        String lastName,
        String username,
        String email,
        String password) {

    @Override
    public String toString() {
        return "RegisterUserCommand[redacted]";
    }
}
