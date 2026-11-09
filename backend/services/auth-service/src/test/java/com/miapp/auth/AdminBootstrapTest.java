package com.miapp.auth;

import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.application.service.AuthAuditService;
import com.miapp.auth.domain.model.Account;
import com.miapp.auth.domain.model.AccountStatus;
import com.miapp.auth.domain.model.Role;
import com.miapp.auth.infrastructure.config.AdminBootstrap;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminBootstrapTest {
    @Test
    void bootstrapRecordsSystemAssignmentAndRejectsAnAuditorAccount() {
        var accounts = mock(AccountPersistencePort.class);
        var roles = mock(RolePersistencePort.class);
        var audit = mock(AuthAuditService.class);
        UUID user = UUID.randomUUID();
        Instant now = Instant.now();
        when(accounts.findByUsername("bootstrap")).thenReturn(Optional.of(new Account(
                UUID.randomUUID(), user, "bootstrap", "bootstrap@example.com", "hash",
                AccountStatus.ACTIVE, now, now)));
        var bootstrap = new AdminBootstrap(accounts, roles, audit, "bootstrap");

        when(roles.findRolesByUserId(user)).thenReturn(EnumSet.of(Role.SECURITY_AUDITOR));
        bootstrap.bootstrap();
        verify(roles, never()).assignRole(any(), any(), any());

        when(roles.findRolesByUserId(user)).thenReturn(EnumSet.of(Role.NORMAL_USER));
        bootstrap.bootstrap();
        verify(roles).assignRole(user, user, Role.ADMIN);
        verify(audit).recordSystemRoleAssignment(user, Role.ADMIN);
    }
}
