package com.miapp.auth.application.service;

import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.application.port.out.SessionPersistencePort;
import com.miapp.auth.application.port.out.UserAdministrationPort;
import com.miapp.auth.domain.exception.InvalidRoleOperationException;
import com.miapp.auth.domain.model.Account;
import com.miapp.auth.domain.model.AccountStatus;
import com.miapp.auth.domain.model.AuditEvent;
import com.miapp.auth.domain.model.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Coordinates account state, session revocation and audit in one transaction. */
@Service
public class UserAdministrationService {
    private final AccountPersistencePort accounts;
    private final SessionPersistencePort sessions;
    private final RolePersistencePort roles;
    private final UserAdministrationPort users;
    private final AuthAuditService audit;
    private final Clock clock;

    public UserAdministrationService(AccountPersistencePort accounts, SessionPersistencePort sessions,
                                     RolePersistencePort roles, UserAdministrationPort users,
                                     AuthAuditService audit, Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.roles = roles;
        this.users = users;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page users(UUID actor, int page, int size) {
        requireAdmin(actor);
        if (page < 0 || page > 1_000_000 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination");
        }
        var rows = users.findUsers(size + 1, (long) page * size);
        boolean hasNext = rows.size() > size;
        List<UserDto> items = (hasNext ? rows.subList(0, size) : rows).stream()
                .map(row -> new UserDto(row.id(), row.firstName(), row.lastName(), row.username(), row.email(),
                        roles.findRolesByUserId(row.id()), row.status(), row.createdAt())).toList();
        return new Page(items, page, size, hasNext);
    }

    @Transactional
    public void suspend(UUID actor, UUID target) {
        changeStatus(actor, target, AccountStatus.ACTIVE, AccountStatus.LOCKED);
    }

    @Transactional
    public void reactivate(UUID actor, UUID target) {
        changeStatus(actor, target, AccountStatus.LOCKED, AccountStatus.ACTIVE);
    }

    private void changeStatus(UUID actor, UUID target, AccountStatus expected, AccountStatus next) {
        roles.lockAdministratorInvariant();
        requireAdmin(actor);
        Account account = accounts.findByUserIdForUpdate(target).orElseThrow(
                () -> new InvalidRoleOperationException("The account cannot be changed"));
        if (account.status() != expected) throw new InvalidRoleOperationException("The account cannot be changed");
        if (next == AccountStatus.LOCKED && roles.hasAdminRole(target)
                && roles.countActiveAdministrators() <= 1) {
            throw new InvalidRoleOperationException("The last active administrator cannot be removed");
        }
        Instant now = clock.instant();
        accounts.save(new Account(account.id(), account.userId(), account.username(), account.email(),
                account.passwordHash(), next, account.createdAt(), now));
        if (next == AccountStatus.LOCKED) sessions.revokeAllForAccount(account.id(), now);
        audit.record(next == AccountStatus.LOCKED ? AuditEvent.Type.USER_SUSPENDED : AuditEvent.Type.USER_REACTIVATED,
                actor, target, AuditEvent.Resource.USER, target);
    }

    private void requireAdmin(UUID actor) {
        if (!roles.hasAdminRole(actor)) throw new InvalidRoleOperationException("Administrator role required");
    }

    public record UserDto(UUID id, String firstName, String lastName, String username, String email,
                          Set<Role> roles, AccountStatus status, Instant createdAt) {}
    public record Page(List<UserDto> items, int page, int size, boolean hasNext) {}
}
