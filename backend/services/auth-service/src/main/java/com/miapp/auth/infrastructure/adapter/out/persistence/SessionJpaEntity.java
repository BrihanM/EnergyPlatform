package com.miapp.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA aislada para la tabla {@code sessions}.
 */
@Entity
@Table(name = "sessions")
public class SessionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected SessionJpaEntity() {
    }

    public SessionJpaEntity(UUID id, UUID accountId, String refreshTokenHash, Instant expiresAt,
                            Instant createdAt, Instant updatedAt, Instant revokedAt) {
        this.id = id;
        this.accountId = accountId;
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.revokedAt = revokedAt;
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
