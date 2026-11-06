package com.miapp.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.time.Instant;

@Entity
@Table(name = "user_roles")
class UserRoleJpaEntity {

    @EmbeddedId
    private UserRoleId id;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected UserRoleJpaEntity() {
    }

    UserRoleJpaEntity(UserRoleId id, Instant assignedAt) {
        this.id = id;
        this.assignedAt = assignedAt;
    }

    UserRoleId getId() {
        return id;
    }
}
