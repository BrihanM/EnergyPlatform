package com.miapp.auth;

import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.application.port.out.UserPersistencePort;
import com.miapp.auth.application.service.RoleManagementService;
import com.miapp.auth.domain.exception.InvalidRoleOperationException;
import com.miapp.auth.domain.model.Role;
import com.miapp.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RoleManagementServiceTest {

    private RolePersistencePort roles;
    private UserPersistencePort users;
    private RoleManagementService service;
    private UUID administrator;
    private UUID target;

    @BeforeEach
    void setUp() {
        roles = mock(RolePersistencePort.class);
        users = mock(UserPersistencePort.class);
        service = new RoleManagementService(roles, users);
        administrator = UUID.randomUUID();
        target = UUID.randomUUID();
        User targetUser = new User(target, "Target", "User", Instant.now(), Instant.now());
        when(users.findById(target)).thenReturn(Optional.of(targetUser));
    }

    @Test
    void rejectsRoleChangesFromNonAdministrator() {
        when(roles.hasAdminRole(administrator)).thenReturn(false);

        assertThatThrownBy(() -> service.assign(administrator, target, Role.SECURITY_AUDITOR))
                .isInstanceOf(InvalidRoleOperationException.class)
                .hasMessage("Administrator role required");

        verify(roles, never()).assignRole(any(), any(), any());
    }

    @Test
    void preventsRevokingTheLastActiveAdministrator() {
        when(roles.hasAdminRole(administrator)).thenReturn(true);
        when(roles.hasAdminRole(target)).thenReturn(true);
        when(roles.countActiveAdministrators()).thenReturn(1L);

        assertThatThrownBy(() -> service.revoke(administrator, target, Role.ADMIN))
                .isInstanceOf(InvalidRoleOperationException.class)
                .hasMessage("The last active administrator cannot be removed");

        verify(roles, never()).revokeRole(any(), any(), any());
    }

    @Test
    void allowsAdministratorToAssignOfficialRole() {
        when(roles.hasAdminRole(administrator)).thenReturn(true);
        when(roles.findRolesByUserId(target)).thenReturn(EnumSet.of(Role.NORMAL_USER, Role.SECURITY_AUDITOR));

        service.assign(administrator, target, Role.SECURITY_AUDITOR);

        verify(roles).assignRole(administrator, target, Role.SECURITY_AUDITOR);
        verify(roles).findRolesByUserId(target);
    }
}
