package com.miapp.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of the closed role catalogue.
 */
@Entity
@Table(name = "roles")
class RoleJpaEntity {

    @Id
    @Column(name = "code", length = 32)
    private String code;

    protected RoleJpaEntity() {
    }

    RoleJpaEntity(String code) {
        this.code = code;
    }

    String getCode() {
        return code;
    }
}
