package com.miapp.auth;

import com.miapp.auth.application.port.out.UserPersistencePort;
import com.miapp.auth.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que los nombres de usuario se almacenan cifrados con pgcrypto y solo se descifran
 * al recuperar el usuario a través del puerto de persistencia (HU-18).
 */
@SpringBootTest
@ActiveProfiles("testcontainers")
@Testcontainers(disabledWithoutDocker = true)
class UserNameEncryptionIntegrationTest extends PostgresIntegrationTest {

    private static final String PLAIN_FIRST_NAME = "Brayan";
    private static final String PLAIN_LAST_NAME = "Marín";

    @Autowired
    private UserPersistencePort userPersistencePort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void storesNamesAsCiphertextAndRestoresThemForAuthorizedRead() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-15T10:00:00Z");
        userPersistencePort.save(new User(id, PLAIN_FIRST_NAME, PLAIN_LAST_NAME, now, now));

        Long plaintextMatches = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ? "
                        + "AND (position(convert_to(?, 'UTF8') IN first_name_enc) > 0 "
                        + "OR position(convert_to(?, 'UTF8') IN last_name_enc) > 0)",
                Long.class, id, PLAIN_FIRST_NAME, PLAIN_LAST_NAME);
        assertThat(plaintextMatches).isZero();

        User restored = userPersistencePort.findById(id).orElseThrow();
        assertThat(restored.firstName()).isEqualTo(PLAIN_FIRST_NAME);
        assertThat(restored.lastName()).isEqualTo(PLAIN_LAST_NAME);
    }

    @Test
    void updatesEncryptedNamesOnSecondSave() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-15T10:00:00Z");
        userPersistencePort.save(new User(id, "Ana", "Pérez", now, now));
        userPersistencePort.save(new User(id, "Ana María", "Pérez López", now, now.plusSeconds(60)));

        User restored = userPersistencePort.findById(id).orElseThrow();
        assertThat(restored.firstName()).isEqualTo("Ana María");
        assertThat(restored.lastName()).isEqualTo("Pérez López");
    }

    @Test
    void preservesNonAsciiCharactersWithoutSanitizing() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-15T10:00:00Z");
        String name = "José Ñandú";
        userPersistencePort.save(new User(id, name, "O'Brien-Smith", now, now));

        User restored = userPersistencePort.findById(id).orElseThrow();
        assertThat(restored.firstName()).isEqualTo(name);
        assertThat(restored.lastName()).isEqualTo("O'Brien-Smith");
    }

    @Test
    void returnsEmptyWhenUserDoesNotExist() {
        assertThat(userPersistencePort.findById(UUID.randomUUID())).isEmpty();
    }
}
