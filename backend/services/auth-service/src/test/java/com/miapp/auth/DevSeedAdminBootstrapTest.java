package com.miapp.auth;

import com.miapp.auth.application.command.RegisterUserCommand;
import com.miapp.auth.application.port.in.RegisterUserUseCase;
import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.application.service.AuthAuditService;
import com.miapp.auth.domain.model.Account;
import com.miapp.auth.domain.model.AccountStatus;
import com.miapp.auth.domain.model.Role;
import com.miapp.auth.infrastructure.config.DevSeedAdminBootstrap;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DevSeedAdminBootstrapTest {

    private final RegisterUserUseCase registerUser = mock(RegisterUserUseCase.class);
    private final AccountPersistencePort accounts = mock(AccountPersistencePort.class);
    private final RolePersistencePort roles = mock(RolePersistencePort.class);
    private final AuthAuditService audit = mock(AuthAuditService.class);

    private DevSeedAdminBootstrap bootstrap(String password) {
        return new DevSeedAdminBootstrap(registerUser, accounts, roles, audit,
                "demo-admin", "demo-admin@example.test", password);
    }

    private static Account account(UUID user, AccountStatus status) {
        Instant now = Instant.now();
        return new Account(UUID.randomUUID(), user, "demo-admin", "demo-admin@example.test",
                "hash", status, now, now);
    }

    @Test
    void registersAndPromotesDemoAdminWhenNoAdministratorExists() {
        UUID user = UUID.randomUUID();
        when(roles.countActiveAdministrators()).thenReturn(0L);
        when(accounts.findByUsername("demo-admin"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(account(user, AccountStatus.ACTIVE)));
        when(roles.findRolesByUserId(user)).thenReturn(EnumSet.of(Role.NORMAL_USER));

        bootstrap("Demo-Passw0rd!").seedAdministrator();

        verify(registerUser).register(any(RegisterUserCommand.class));
        verify(roles).assignRole(user, user, Role.ADMIN);
        verify(audit).recordSystemRoleAssignment(user, Role.ADMIN);
    }

    @Test
    void isIdempotentWhenAnActiveAdministratorAlreadyExists() {
        when(roles.countActiveAdministrators()).thenReturn(1L);

        bootstrap("Demo-Passw0rd!").seedAdministrator();

        verify(registerUser, never()).register(any());
        verify(roles, never()).assignRole(any(), any(), any());
    }

    @Test
    void doesNotPromoteExistingAuditorAccount() {
        UUID user = UUID.randomUUID();
        when(roles.countActiveAdministrators()).thenReturn(0L);
        when(accounts.findByUsername("demo-admin")).thenReturn(Optional.of(account(user, AccountStatus.ACTIVE)));
        when(roles.findRolesByUserId(user)).thenReturn(EnumSet.of(Role.SECURITY_AUDITOR));

        bootstrap("Demo-Passw0rd!").seedAdministrator();

        verify(roles, never()).assignRole(any(), any(), any());
    }

    @Test
    void failsFastWhenPasswordIsMissing() {
        assertThrows(IllegalStateException.class, () -> bootstrap("").seedAdministrator());
        verifyNoInteractions(registerUser);
    }
}
