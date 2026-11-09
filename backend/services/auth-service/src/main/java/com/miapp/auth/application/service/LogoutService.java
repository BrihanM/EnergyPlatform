package com.miapp.auth.application.service;

import com.miapp.auth.application.port.in.LogoutUseCase;
import com.miapp.auth.application.port.out.SessionPersistencePort;
import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.domain.exception.InvalidSessionException;
import com.miapp.auth.domain.model.AuditEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revokes the session identified by a current or retained rotated refresh token.
 */
@Service
public class LogoutService implements LogoutUseCase {

    private final SessionPersistencePort sessions;
    private final SessionService sessionService;
    private final AccountPersistencePort accounts;
    private final AuthAuditService audit;

    public LogoutService(SessionPersistencePort sessions, SessionService sessionService,
                         AccountPersistencePort accounts, AuthAuditService audit) {
        this.sessions = sessions;
        this.sessionService = sessionService;
        this.accounts = accounts;
        this.audit = audit;
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidSessionException();
        }
        String hash = sessionService.hash(refreshToken);
        var reference = sessions.findForLogoutByHash(hash).orElseThrow(InvalidSessionException::new);
        var account = accounts.findByIdForUpdate(reference.accountId()).orElseThrow(InvalidSessionException::new);
        var session = sessions.findByIdForUpdate(reference.id()).orElseThrow(InvalidSessionException::new);
        if (!session.accountId().equals(account.id())
                || !(sessionService.matches(refreshToken, session)
                || sessions.rotatedHashBelongsToSession(session.id(), hash))) {
            throw new InvalidSessionException();
        }
        if (session.revokedAt() == null) {
            sessionService.revoke(session);
            audit.record(AuditEvent.Type.LOGOUT, account.userId(), account.userId(),
                    AuditEvent.Resource.SESSION, session.id());
        }
    }
}
