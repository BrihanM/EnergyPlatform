package com.miapp.auth;

import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.application.port.out.UserPersistencePort;
import com.miapp.auth.domain.model.Account;
import com.miapp.auth.domain.model.AccountStatus;
import com.miapp.auth.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica la persistencia de los contratos de salida contra PostgreSQL real.
 *
 * <p>La clase se ejecuta con el perfil de Testcontainers para no depender de una
 * base de datos manual.</p>
 */
@SpringBootTest
@ActiveProfiles("testcontainers")
@Testcontainers(disabledWithoutDocker = true)
class PersistenceAdapterIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private UserPersistencePort userPersistencePort;

    @Autowired
    private AccountPersistencePort accountPersistencePort;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsUserAndAccountWithTheirRelationship() {
        User user = User.create("Ada", "Lovelace");
        Account account = new Account(
                java.util.UUID.randomUUID(), user.id(), "ada", "ada@example.com", "bcrypt-hash",
                AccountStatus.ACTIVE, Instant.now(), Instant.now());

        userPersistencePort.save(user);
        accountPersistencePort.save(account);

        assertThat(userPersistencePort.findById(user.id())).contains(user);
        assertThat(accountPersistencePort.findByUsername("ada"))
                .contains(account);
    }

    @Test
    @Transactional
    void enforcesUniqueLoginIdentifier() {
        User user = userPersistencePort.save(User.create("Grace", "Hopper"));
        accountPersistencePort.save(new Account(
                java.util.UUID.randomUUID(), user.id(), "grace", "grace@example.com", "hash-1",
                AccountStatus.ACTIVE, Instant.now(), Instant.now()));

        Account duplicate = new Account(
                java.util.UUID.randomUUID(), user.id(), "grace", "duplicate@example.com", "hash-2",
                AccountStatus.ACTIVE, Instant.now(), Instant.now());

        assertThatThrownBy(() -> {
            accountPersistencePort.save(duplicate);
            entityManager.flush();
        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
