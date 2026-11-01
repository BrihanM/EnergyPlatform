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
    private final String firstName;
    private final String lastName;
    private final Instant createdAt;
    private final Instant updatedAt;

    /**
     * Crea una identidad con un identificador distribuido y sus datos básicos.
     *
     * @param id identificador único de la identidad
     * @param firstName nombre de la identidad
     * @param lastName apellido de la identidad
     * @param createdAt instante de creación
     * @param updatedAt instante de última actualización
     */
    public User(UUID id, String firstName, String lastName, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * Crea una identidad nueva usando el instante actual como fecha de auditoría.
     *
     * @param firstName nombre de la identidad
     * @param lastName apellido de la identidad
     * @return identidad de dominio nueva
     */
    public static User create(String firstName, String lastName) {
        Instant now = Instant.now();
        return new User(UUID.randomUUID(), firstName, lastName, now, now);
    }

    public UUID id() {
        return id;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
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

    @Override
    public boolean equals(Object other) {
        return other instanceof User user && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
