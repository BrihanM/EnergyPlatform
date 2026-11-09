package com.miapp.auth.infrastructure.adapter.out.persistence;

import com.miapp.auth.application.port.out.SessionPersistencePort;
import com.miapp.auth.domain.model.Session;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Timestamp;
import java.time.Instant;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia de sesiones mediante Spring Data JPA.
 */
@Component
public class SessionPersistenceAdapter implements SessionPersistencePort {

    private final SessionJpaRepository repository;
    private final JdbcTemplate jdbc;

    public SessionPersistenceAdapter(SessionJpaRepository repository, JdbcTemplate jdbc) {
        this.repository = repository;
        this.jdbc = jdbc;
    }

    @Override
    public Session save(Session session) {
        return SessionPersistenceMapper.toDomain(repository.save(SessionPersistenceMapper.toEntity(session)));
    }

    @Override
    public Optional<Session> findById(UUID sessionId) {
        return repository.findById(sessionId).map(SessionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Session> findByIdForUpdate(UUID sessionId) {
        return repository.findByIdForUpdate(sessionId).map(SessionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<SessionReference> findCurrentByHash(String hash) {
        var matches = jdbc.query("SELECT id, account_id FROM sessions WHERE refresh_token_hash = ?",
                (rs, row) -> new SessionReference(rs.getObject("id", UUID.class),
                        rs.getObject("account_id", UUID.class)), hash);
        if (matches.size() > 1) throw new IllegalStateException("Refresh token hash belongs to multiple sessions");
        return matches.stream().findFirst();
    }

    @Override
    public Optional<SessionReference> findForLogoutByHash(String hash) {
        var matches = jdbc.query("""
                SELECT s.id, s.account_id FROM sessions s
                WHERE s.refresh_token_hash = ?
                UNION
                SELECT s.id, s.account_id FROM rotated_refresh_token_history h
                JOIN sessions s ON s.id = h.session_id
                WHERE h.refresh_token_hash = ? AND s.expires_at > CURRENT_TIMESTAMP
                """, (rs, row) -> new SessionReference(rs.getObject("id", UUID.class),
                rs.getObject("account_id", UUID.class)), hash, hash);
        if (matches.size() > 1) throw new IllegalStateException("Refresh token hash belongs to multiple sessions");
        return matches.stream().findFirst();
    }

    @Override
    public boolean rotatedHashBelongsToSession(UUID sessionId, String hash) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM rotated_refresh_token_history
                WHERE session_id = ? AND refresh_token_hash = ?)
                """, Boolean.class, sessionId, hash));
    }

    @Override
    public void rememberRotatedHash(Session session, Instant rotatedAt) {
        jdbc.update("""
                INSERT INTO rotated_refresh_token_history (refresh_token_hash, session_id, rotated_at)
                VALUES (?, ?, ?)
                """, session.refreshTokenHash(), session.id(), Timestamp.from(rotatedAt));
    }

    @Override
    public int deleteExpiredRotatedHashes(int sessionLimit) {
        return jdbc.update("""
                WITH expired AS (
                    SELECT s.id FROM sessions s
                    WHERE s.expires_at <= CURRENT_TIMESTAMP
                      AND EXISTS (SELECT 1 FROM rotated_refresh_token_history h WHERE h.session_id = s.id)
                    ORDER BY s.expires_at
                    LIMIT ?
                    FOR UPDATE OF s SKIP LOCKED
                )
                DELETE FROM rotated_refresh_token_history h USING expired e WHERE h.session_id = e.id
                """, sessionLimit);
    }

    @Override
    public Optional<Session> findByRefreshTokenHash(String refreshTokenHash) {
        return repository.findByRefreshTokenHash(refreshTokenHash)
                .map(SessionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Session> findByRefreshTokenHashForUpdate(String refreshTokenHash) {
        return repository.findByRefreshTokenHashForUpdate(refreshTokenHash)
                .map(SessionPersistenceMapper::toDomain);
    }

    @Override
    public void revokeAllForAccount(UUID accountId, java.time.Instant now) {
        repository.revokeAllForAccount(accountId, now);
    }
}
