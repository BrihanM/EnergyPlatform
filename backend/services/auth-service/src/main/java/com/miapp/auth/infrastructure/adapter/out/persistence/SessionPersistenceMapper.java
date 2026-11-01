package com.miapp.auth.infrastructure.adapter.out.persistence;

import com.miapp.auth.domain.model.Session;

/**
 * Traduce sesiones de dominio a su representación JPA y viceversa.
 */
final class SessionPersistenceMapper {

    private SessionPersistenceMapper() {
    }

    static SessionJpaEntity toEntity(Session session) {
        return new SessionJpaEntity(session.id(), session.accountId(), session.refreshTokenHash(),
                session.expiresAt(), session.createdAt(), session.updatedAt(), session.revokedAt());
    }

    static Session toDomain(SessionJpaEntity entity) {
        return new Session(entity.getId(), entity.getAccountId(), entity.getRefreshTokenHash(),
                entity.getExpiresAt(), entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getRevokedAt());
    }
}
