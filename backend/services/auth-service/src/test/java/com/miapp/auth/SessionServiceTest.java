package com.miapp.auth;

import com.miapp.auth.application.port.out.RefreshTokenGeneratorPort;
import com.miapp.auth.application.port.out.RefreshTokenHashingPort;
import com.miapp.auth.application.port.out.SessionPersistencePort;
import com.miapp.auth.application.result.CreateSessionResult;
import com.miapp.auth.application.service.SessionProperties;
import com.miapp.auth.application.service.SessionService;
import com.miapp.auth.domain.model.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private SessionPersistencePort persistence;
    private RefreshTokenGeneratorPort generator;
    private RefreshTokenHashingPort hashing;
    private SessionService service;

    @BeforeEach
    void setUp() {
        persistence = mock(SessionPersistencePort.class);
        generator = mock(RefreshTokenGeneratorPort.class);
        hashing = mock(RefreshTokenHashingPort.class);
        service = new SessionService(persistence, generator, hashing,
                new SessionProperties("604800000"),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsSessionWithHashedTokenAndConfigurableExpiration() {
        UUID accountId = UUID.randomUUID();
        when(generator.generate()).thenReturn("raw-token");
        when(hashing.hash("raw-token")).thenReturn("hashed-token");
        when(persistence.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateSessionResult result = service.create(accountId);

        assertThat(result.refreshToken()).isEqualTo("raw-token");
        assertThat(result.session().accountId()).isEqualTo(accountId);
        assertThat(result.session().refreshTokenHash()).isEqualTo("hashed-token");
        assertThat(result.session().expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        verify(persistence).save(result.session());
        assertThat(result.toString()).doesNotContain("raw-token");
    }

    @Test
    void supportsRevocationAndRefreshTokenReplacement() {
        Session session = Session.create(UUID.randomUUID(), "old-hash",
                NOW.plus(Duration.ofDays(7)), NOW);

        Session replaced = session.replaceRefreshToken("new-hash",
                NOW.plus(Duration.ofDays(14)), NOW.plusSeconds(1));
        Session revoked = replaced.revoke(NOW.plusSeconds(2));

        assertThat(replaced.refreshTokenHash()).isEqualTo("new-hash");
        assertThat(replaced.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(14)));
        assertThat(revoked.revokedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(revoked.isActiveAt(NOW.plusSeconds(3))).isFalse();
    }
}
