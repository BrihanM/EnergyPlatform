package com.miapp.auth.application.service;

import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.application.port.out.SessionPersistencePort;
import com.miapp.auth.application.port.out.UserPersistencePort;
import com.miapp.auth.domain.exception.InvalidSessionException;
import com.miapp.auth.domain.model.AccountStatus;
import com.miapp.auth.domain.model.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

/** Resolves current authorization for an already verified token identity. */
@Service
public class SessionAuthorizationService {
    private final SessionPersistencePort sessions;
    private final AccountPersistencePort accounts;
    private final UserPersistencePort users;
    private final RolePersistencePort roles;
    private final Clock clock;

    public SessionAuthorizationService(SessionPersistencePort sessions, AccountPersistencePort accounts,
                                       UserPersistencePort users, RolePersistencePort roles, Clock clock) {
        this.sessions = sessions;
        this.accounts = accounts;
        this.users = users;
        this.roles = roles;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Authorization validate(UUID userId, UUID sessionId) {
        var session = sessions.findById(sessionId)
                .filter(value -> value.isActiveAt(clock.instant()))
                .orElseThrow(InvalidSessionException::new);
        var account = accounts.findById(session.accountId())
                .filter(value -> value.status() == AccountStatus.ACTIVE && value.userId().equals(userId))
                .orElseThrow(InvalidSessionException::new);
        users.findById(userId).orElseThrow(InvalidSessionException::new);
        return new Authorization(userId, account.username(), roles.findRolesByUserId(userId));
    }

    public record Authorization(UUID userId, String username, Set<Role> roles) {}
}
