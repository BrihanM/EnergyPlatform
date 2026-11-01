package com.miapp.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Cuenta utilizada para acceder al sistema.
 *
 * <p>Separa el identificador de acceso y el hash de credencial de los datos
 * generales de {@link User}. El dominio nunca recibe ni almacena contraseñas
 * en texto plano.</p>
 */
public final class Account {

    private final UUID id;
    private final UUID userId;
    private final String loginIdentifier;
    private final String passwordHash;
    private final AccountStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    /**
     * Construye una cuenta a partir de datos persistibles del dominio.
     *
     * @param id identificador único de la cuenta
     * @param userId identidad propietaria
     * @param loginIdentifier identificador único de acceso
     * @param passwordHash hash seguro de la credencial
     * @param status estado operativo
     * @param createdAt instante de creación
     * @param updatedAt instante de actualización
     */
    public Account(UUID id, UUID userId, String loginIdentifier, String passwordHash,
                   AccountStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.loginIdentifier = requireText(loginIdentifier, "loginIdentifier");
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String loginIdentifier() {
        return loginIdentifier;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public AccountStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
