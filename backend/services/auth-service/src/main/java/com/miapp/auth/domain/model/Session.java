package com.miapp.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Sesión autenticada asociada a una cuenta.
 *
 * <p>La sesión sólo conserva el hash del refresh token. El token original
 * existe únicamente durante su creación y nunca forma parte del dominio
 * persistible.</p>
 */
public final class Session {

    private final UUID id;
    private final UUID accountId;
    private final String refreshTokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant revokedAt;

    public Session(UUID id, UUID accountId, String refreshTokenHash, Instant expiresAt,
                   Instant createdAt, Instant updatedAt, Instant revokedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.refreshTokenHash = requireText(refreshTokenHash, "refreshTokenHash");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.revokedAt = revokedAt;
        if (expiresAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("expiresAt must not be before createdAt");
        }
    }

    /**
     * Crea una sesión activa con la expiración calculada por la aplicación.
     */
    public static Session create(UUID accountId, String refreshTokenHash,
                                 Instant expiresAt, Instant now) {
        return new Session(UUID.randomUUID(), accountId, refreshTokenHash,
                expiresAt, now, now, null);
    }

    public UUID id() {
        return id;
    }

    public UUID accountId() {
        return accountId;
    }

    public String refreshTokenHash() {
        return refreshTokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    /**
     * Indica si la sesión puede utilizarse en este instante.
     */
    public boolean isActiveAt(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return revokedAt == null && now.isBefore(expiresAt);
    }

    /**
     * Revoca lógicamente la sesión sin eliminar su registro.
     */
    public Session revoke(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new Session(id, accountId, refreshTokenHash, expiresAt,
                createdAt, now, revokedAt == null ? now : revokedAt);
    }

    /**
     * Reemplaza el hash y la expiración para permitir rotación futura.
     */
    public Session replaceRefreshToken(String newRefreshTokenHash, Instant newExpiresAt, Instant now) {
        return new Session(id, accountId, newRefreshTokenHash, newExpiresAt,
                createdAt, now, revokedAt);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Session session && id.equals(session.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
