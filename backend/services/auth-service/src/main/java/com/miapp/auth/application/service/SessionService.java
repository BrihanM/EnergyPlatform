package com.miapp.auth.application.service;

import com.miapp.auth.application.port.out.RefreshTokenGeneratorPort;
import com.miapp.auth.application.port.out.RefreshTokenHashingPort;
import com.miapp.auth.application.port.out.SessionPersistencePort;
import com.miapp.auth.application.result.CreateSessionResult;
import com.miapp.auth.domain.model.Session;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de aplicación para crear y persistir sesiones de autenticación.
 */
@Service
public class SessionService {

    private final SessionPersistencePort sessionPersistence;
    private final RefreshTokenGeneratorPort tokenGenerator;
    private final RefreshTokenHashingPort tokenHashing;
    private final Duration expiration;
    private final Clock clock;

    public SessionService(SessionPersistencePort sessionPersistence,
                          RefreshTokenGeneratorPort tokenGenerator,
                          RefreshTokenHashingPort tokenHashing,
                          SessionProperties properties,
                          Clock clock) {
        this.sessionPersistence = sessionPersistence;
        this.tokenGenerator = tokenGenerator;
        this.tokenHashing = tokenHashing;
        this.expiration = properties.duration();
        this.clock = clock;
    }

    /**
     * Genera un token opaco, persiste sólo su hash y devuelve el valor real.
     */
    public CreateSessionResult create(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Instant now = clock.instant();
        String refreshToken = tokenGenerator.generate();
        Session session = Session.create(accountId, tokenHashing.hash(refreshToken),
                now.plus(expiration), now);
        return new CreateSessionResult(sessionPersistence.save(session), refreshToken);
    }

    public Session revoke(Session session) {
        return sessionPersistence.save(session.revoke(clock.instant()));
    }

    public String generateReplacementToken(Session session) {
        String refreshToken = tokenGenerator.generate();
        Session rotated = session.replaceRefreshToken(tokenHashing.hash(refreshToken),
                clock.instant().plus(expiration), clock.instant());
        sessionPersistence.save(rotated);
        return refreshToken;
    }

    public boolean matches(String refreshToken, Session session) {
        return tokenHashing.matches(refreshToken, session.refreshTokenHash());
    }

    public String hash(String refreshToken) {
        return tokenHashing.hash(refreshToken);
    }
}
