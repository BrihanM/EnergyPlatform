package com.miapp.auth.application.result;

import com.miapp.auth.domain.model.Session;

import java.util.Objects;

/**
 * Resultado de crear una sesión. El token real se entrega sólo al adaptador
 * que lo enviará mediante una cookie segura.
 */
public final class CreateSessionResult {

    private final Session session;
    private final String refreshToken;

    public CreateSessionResult(Session session, String refreshToken) {
        this.session = Objects.requireNonNull(session, "session must not be null");
        this.refreshToken = Objects.requireNonNull(refreshToken, "refreshToken must not be null");
    }

    public Session session() {
        return session;
    }

    public String refreshToken() {
        return refreshToken;
    }

    @Override
    public String toString() {
        return "CreateSessionResult{session=" + session.id() + ", refreshToken=[REDACTED]}";
    }
}
