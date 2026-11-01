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
}
