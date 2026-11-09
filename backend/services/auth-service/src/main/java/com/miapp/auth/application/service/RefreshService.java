package com.miapp.auth.application.service;

import com.miapp.auth.application.port.in.RefreshSessionUseCase;
import com.miapp.auth.application.port.out.AccessTokenGeneratorPort;
import com.miapp.auth.application.port.out.SessionPersistencePort;
import com.miapp.auth.application.port.out.UserPersistencePort;
import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.application.result.AuthenticationResult;
import com.miapp.auth.domain.model.Session;
import com.miapp.auth.domain.model.User;
import com.miapp.auth.domain.exception.InvalidSessionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Valida una sesión y rota su refresh token de forma transaccional.
 */
@Service
public class RefreshService implements RefreshSessionUseCase {

    private final SessionPersistencePort sessions;
    private final SessionService sessionService;
    private final UserPersistencePort users;
    private final AccountPersistencePort accounts;
    private final AccessTokenGeneratorPort accessTokens;
    private final Clock clock;
    private final RolePersistencePort roles;

    public RefreshService(SessionPersistencePort sessions, SessionService sessionService,
                          UserPersistencePort users, AccountPersistencePort accounts,
                          AccessTokenGeneratorPort accessTokens,
                          Clock clock,
                          RolePersistencePort roles) {
        this.sessions = sessions;
        this.sessionService = sessionService;
        this.users = users;
        this.accounts = accounts;
        this.accessTokens = accessTokens;
        this.clock = clock;
        this.roles = roles;
    }

    @Override
    @Transactional
    public AuthenticationResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidSessionException();
        }
        String hash = sessionServiceHash(refreshToken);
        Session session = sessions.findByRefreshTokenHash(hash)
                .filter(value -> value.isActiveAt(clock.instant()))
                .orElseThrow(InvalidSessionException::new);
        if (!sessionService.matches(refreshToken, session)) {
            throw new InvalidSessionException();
        }
        String replacement = sessionService.generateReplacementToken(session);
        var account = accounts.findById(session.accountId()).orElseThrow(InvalidSessionException::new);
        User user = users.findById(account.userId()).orElseThrow(InvalidSessionException::new);
        var currentRoles = roles.findRolesByUserId(user.id());
        AccessTokenGeneratorPort.AccessToken access = accessTokens.generate(
                user.id(), account.username(), currentRoles, clock.instant());
        return new AuthenticationResult(access.value(), access.expiresInSeconds(), replacement,
                new AuthenticationResult.UserSummary(user.id(), user.firstName(), user.lastName(),
                        account.username(), account.email(), user.createdAt()), currentRoles);
    }

    private String sessionServiceHash(String refreshToken) {
        return sessionService.hash(refreshToken);
    }
}
