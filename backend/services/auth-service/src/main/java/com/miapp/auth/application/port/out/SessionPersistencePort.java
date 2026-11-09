package com.miapp.auth.application.port.out;

import com.miapp.auth.domain.model.Session;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para las operaciones necesarias sobre sesiones.
 */
public interface SessionPersistencePort {

    Session save(Session session);

    Optional<Session> findById(UUID sessionId);

    /**
     * Busca una sesión por el hash persistido del token.
     */
    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

    Optional<Session> findByRefreshTokenHashForUpdate(String refreshTokenHash);
}
