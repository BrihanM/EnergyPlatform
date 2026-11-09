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

    Optional<Session> findByIdForUpdate(UUID sessionId);

    record SessionReference(UUID id, UUID accountId) {}

    Optional<SessionReference> findCurrentByHash(String refreshTokenHash);

    Optional<SessionReference> findForLogoutByHash(String refreshTokenHash);

    boolean rotatedHashBelongsToSession(UUID sessionId, String refreshTokenHash);

    void rememberRotatedHash(Session session, java.time.Instant rotatedAt);

    int deleteExpiredRotatedHashes(int sessionLimit);

    /**
     * Busca una sesión por el hash persistido del token.
     */
    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

    Optional<Session> findByRefreshTokenHashForUpdate(String refreshTokenHash);

    void revokeAllForAccount(UUID accountId, java.time.Instant now);
}
