package com.miapp.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
class UserRoleId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "role_code", length = 32)
    private String roleCode;

    protected UserRoleId() {
    }

    UserRoleId(UUID userId, String roleCode) {
        this.userId = userId;
        this.roleCode = roleCode;
    }

    UUID getUserId() {
        return userId;
    }

    String getRoleCode() {
        return roleCode;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof UserRoleId value
                && userId.equals(value.userId) && roleCode.equals(value.roleCode);
    }

    @Override
    public int hashCode() {
        return 31 * userId.hashCode() + roleCode.hashCode();
    }
}
