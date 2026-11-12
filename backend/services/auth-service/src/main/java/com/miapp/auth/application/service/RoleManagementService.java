package com.miapp.auth.application.service;

import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.application.port.out.UserPersistencePort;
import com.miapp.auth.domain.exception.InvalidRoleOperationException;
import com.miapp.auth.domain.model.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Application service enforcing administrative role-management rules.
 */
@Service
public class RoleManagementService {

    private final RolePersistencePort roles;
    private final UserPersistencePort users;
    private final AuthAuditService audit;

    public RoleManagementService(RolePersistencePort roles, UserPersistencePort users, AuthAuditService audit) {
        this.roles = roles;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Set<Role> availableRoles() {
        return roles.findAllRoles();
    }

    @Transactional(readOnly = true)
    public Set<Role> rolesOf(UUID actorId, UUID targetId) {
        requireAdmin(actorId);
        requireAccount(targetId);
        return roles.findRolesByUserId(targetId);
    }

    @Transactional
    public Set<Role> assign(UUID actorId, UUID targetId, Role role) {
        roles.lockAdministratorInvariant();
        requireAdmin(actorId);
        requireAccount(targetId);
        Set<Role> currentRoles = roles.findRolesByUserId(targetId);
        if ((role == Role.SECURITY_AUDITOR && currentRoles.contains(Role.ADMIN))
                || (role == Role.ADMIN && currentRoles.contains(Role.SECURITY_AUDITOR))) {
            throw new InvalidRoleOperationException("Administrator and auditor roles cannot be combined");
        }
        boolean alreadyAssigned = currentRoles.contains(role);
        roles.assignRole(actorId, targetId, role);
        if (!alreadyAssigned) {
            audit.recordRole(com.miapp.auth.domain.model.AuditEvent.Type.ROLE_ASSIGNED, actorId, targetId, role);
        }
        return roles.findRolesByUserId(targetId);
    }

    @Transactional
    public Set<Role> revoke(UUID actorId, UUID targetId, Role role) {
        roles.lockAdministratorInvariant();
        requireAdmin(actorId);
        requireAccount(targetId);
        boolean assigned = roles.findRolesByUserId(targetId).contains(role);
        if (role == Role.ADMIN && roles.hasAdminRole(targetId)
                && roles.countActiveAdministrators() <= 1) {
            throw new InvalidRoleOperationException("The last active administrator cannot be removed");
        }
        roles.revokeRole(actorId, targetId, role);
        if (assigned) {
            audit.recordRole(com.miapp.auth.domain.model.AuditEvent.Type.ROLE_REVOKED, actorId, targetId, role);
        }
        return roles.findRolesByUserId(targetId);
    }

    private void requireAdmin(UUID actorId) {
        if (!roles.hasAdminRole(actorId)) {
            throw new InvalidRoleOperationException("Administrator role required");
        }
    }

    private void requireAccount(UUID userId) {
        users.findById(userId).orElseThrow(() ->
                new InvalidRoleOperationException("Target account was not found"));
    }
}
