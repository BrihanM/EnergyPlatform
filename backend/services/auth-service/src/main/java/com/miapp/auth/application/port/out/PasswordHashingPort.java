package com.miapp.auth.application.port.out;

/**
 * Puerto de salida para transformar contraseñas en hashes adaptativos.
 */
public interface PasswordHashingPort {

    /**
     * Genera un hash no reversible para la contraseña recibida.
     *
     * @param rawPassword contraseña recibida durante el registro
     * @return hash seguro
     */
    String hash(String rawPassword);

    /**
     * Comprueba una contraseña sin exponer el algoritmo al caso de uso.
     *
     * @param rawPassword contraseña recibida
     * @param passwordHash hash persistido
     * @return {@code true} cuando la contraseña coincide
     */
    boolean matches(String rawPassword, String passwordHash);
}
