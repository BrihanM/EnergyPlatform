package com.miapp.auth;

import com.miapp.auth.application.port.out.AccessTokenGeneratorPort;
import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.application.port.out.SessionPersistencePort;
import com.miapp.auth.application.port.out.UserPersistencePort;
import com.miapp.auth.application.service.RefreshService;
import com.miapp.auth.application.service.SessionService;
import com.miapp.auth.domain.exception.InvalidSessionException;
import com.miapp.auth.domain.model.Account;
import com.miapp.auth.domain.model.AccountStatus;
import com.miapp.auth.domain.model.Role;
import com.miapp.auth.domain.model.Session;
import com.miapp.auth.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RefreshAccountStatusTest {
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final UUID accountId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Session session = Session.create(accountId, "hash", now.plusSeconds(60), now);
    private final SessionPersistencePort sessions = mock(SessionPersistencePort.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final UserPersistencePort users = mock(UserPersistencePort.class);
    private final AccountPersistencePort accounts = mock(AccountPersistencePort.class);
    private final AccessTokenGeneratorPort tokens = mock(AccessTokenGeneratorPort.class);
    private final RolePersistencePort roles = mock(RolePersistencePort.class);
    private final RefreshService service = new RefreshService(sessions, sessionService, users, accounts,
            tokens, Clock.fixed(now, ZoneOffset.UTC), roles,
            mock(com.miapp.auth.application.service.AuthAuditService.class));

    private void validSession() {
        when(sessionService.hash("refresh")).thenReturn("hash");
        when(sessions.findCurrentByHash("hash")).thenReturn(Optional.of(
                new SessionPersistencePort.SessionReference(session.id(), session.accountId())));
        when(sessions.findByRefreshTokenHashForUpdate("hash")).thenReturn(Optional.of(session));
        when(sessionService.matches("refresh", session)).thenReturn(true);
    }

    private Account account(AccountStatus status) {
        return new Account(accountId, userId, "user", "user@example.com", "hash", status, now, now);
    }

    @Test
    void inactiveAccountsCannotRefreshOrRotateTheirSession() {
        validSession();
        for (AccountStatus status : new AccountStatus[] {AccountStatus.LOCKED, AccountStatus.DISABLED}) {
            when(accounts.findByIdForUpdate(accountId)).thenReturn(Optional.of(account(status)));
            assertThatThrownBy(() -> service.refresh("refresh")).isInstanceOf(InvalidSessionException.class);
        }
        verify(sessionService, never()).generateReplacementToken(any());
        verifyNoInteractions(tokens);
    }

    @Test
    void activeAccountRefreshStillRotatesTheSession() {
        validSession();
        when(accounts.findByIdForUpdate(accountId)).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));
        when(users.findById(userId)).thenReturn(Optional.of(new User(userId, "Test", "User", now, now)));
        when(roles.findRolesByUserId(userId)).thenReturn(Set.of(Role.NORMAL_USER));
        when(sessionService.generateReplacementToken(session)).thenReturn("replacement");
        when(tokens.generate(userId, session.id(), "user", Set.of(Role.NORMAL_USER), now))
                .thenReturn(new AccessTokenGeneratorPort.AccessToken("access", 60));

        assertThat(service.refresh("refresh").accessToken()).isEqualTo("access");
        verify(sessionService).generateReplacementToken(session);
    }
}
