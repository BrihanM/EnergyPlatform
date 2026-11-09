package com.miapp.auth.application.service;

import com.miapp.auth.application.port.in.LogoutUseCase;
import com.miapp.auth.application.port.out.SessionPersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revoca de forma idempotente la sesión indicada por la cookie.
 */
@Service
public class LogoutService implements LogoutUseCase {

    private final SessionPersistencePort sessions;
    private final SessionService sessionService;

    public LogoutService(SessionPersistencePort sessions, SessionService sessionService) {
        this.sessions = sessions;
        this.sessionService = sessionService;
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        sessions.findByRefreshTokenHashForUpdate(sessionService.hash(refreshToken))
                .filter(session -> sessionService.matches(refreshToken, session))
                .ifPresent(sessionService::revoke);
    }
}
