package com.miapp.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Identidad principal de una persona dentro del contexto de autenticación.
 *
 * <p>Es un objeto de dominio independiente de Spring y JPA. Las cuentas de
 * acceso se relacionan con esta identidad mediante su identificador.</p>
 */
public final class User {

    private final UUID id;
    private final String displayName;
    private final Instant createdAt;
    private final Instant updatedAt;

    /**
     * Crea una identidad con un identificador distribuido y sus datos básicos.
     *
     * @param id identificador único de la identidad
     * @param displayName nombre de presentación de la identidad
     * @param createdAt instante de creación
     * @param updatedAt instante de última actualización
     */
    public User(UUID id, String displayName, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.displayName = requireText(displayName, "displayName");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * Crea una identidad nueva usando el instante actual como fecha de auditoría.
     *
     * @param displayName nombre de presentación de la identidad
     * @return identidad de dominio nueva
     */
    public static User create(String displayName) {
        Instant now = Instant.now();
        return new User(UUID.randomUUID(), displayName, now, now);
    }

    public UUID id() {
        return id;
    }

    public String displayName() {
        return displayName;
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
