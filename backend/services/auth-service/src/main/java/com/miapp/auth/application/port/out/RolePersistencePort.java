package com.miapp.auth.application.port.out;

import com.miapp.auth.domain.model.Role;

import java.util.Set;
import java.util.UUID;

/**
 * Persistence contract for role assignments.
 */
public interface RolePersistencePort {

    Set<Role> findRolesByUserId(UUID userId);

    Set<Role> findAllRoles();

    void assignRole(UUID actorUserId, UUID targetUserId, Role role);

    void revokeRole(UUID actorUserId, UUID targetUserId, Role role);

    boolean hasAdminRole(UUID userId);

    long countActiveAdministrators();

    /** Serializes mutations that may change the set of active administrators. */
    void lockAdministratorInvariant();
}
