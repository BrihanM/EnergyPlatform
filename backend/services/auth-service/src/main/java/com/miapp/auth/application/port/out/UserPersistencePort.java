package com.miapp.auth.application.port.out;

import com.miapp.auth.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para almacenar y recuperar identidades del dominio.
 */
public interface UserPersistencePort {

    /**
     * Persiste una identidad.
     *
     * @param user identidad que se almacenará
     * @return identidad persistida
     */
    User save(User user);

    /**
     * Busca una identidad por su identificador.
     *
     * @param id identificador de la identidad
     * @return identidad encontrada, si existe
     */
    Optional<User> findById(UUID id);
}
