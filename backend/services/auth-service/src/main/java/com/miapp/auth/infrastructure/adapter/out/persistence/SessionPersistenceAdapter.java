package com.miapp.auth.infrastructure.adapter.out.persistence;

import com.miapp.auth.application.port.out.SessionPersistencePort;
import com.miapp.auth.domain.model.Session;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia de sesiones mediante Spring Data JPA.
 */
@Component
public class SessionPersistenceAdapter implements SessionPersistencePort {

    private final SessionJpaRepository repository;

    public SessionPersistenceAdapter(SessionJpaRepository repository) {
        this.repository = repository;
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
    public Optional<Session> findByRefreshTokenHash(String refreshTokenHash) {
        return repository.findByRefreshTokenHash(refreshTokenHash)
                .map(SessionPersistenceMapper::toDomain);
    }
}
