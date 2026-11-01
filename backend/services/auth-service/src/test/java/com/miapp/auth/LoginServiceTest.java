package com.miapp.auth;

import com.miapp.auth.application.command.LoginCommand;
import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.application.port.out.PasswordHashingPort;
import com.miapp.auth.application.port.out.UserPersistencePort;
import com.miapp.auth.application.result.LoginResult;
import com.miapp.auth.application.service.LoginService;
import com.miapp.auth.domain.exception.InvalidCredentialsException;
import com.miapp.auth.domain.model.Account;
import com.miapp.auth.domain.model.AccountStatus;
import com.miapp.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del caso de uso de login.
 */
class LoginServiceTest {

    private AccountPersistencePort accountPersistencePort;
    private UserPersistencePort userPersistencePort;
    private PasswordHashingPort passwordHashingPort;
    private LoginService service;
    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        accountPersistencePort = mock(AccountPersistencePort.class);
        userPersistencePort = mock(UserPersistencePort.class);
        passwordHashingPort = mock(PasswordHashingPort.class);
        service = new LoginService(accountPersistencePort, userPersistencePort, passwordHashingPort);
        Instant now = Instant.now();
        user = new User(UUID.randomUUID(), "Brayan", "Marín", now, now);
        account = new Account(UUID.randomUUID(), user.id(), "brayan.marin",
                "brayan@example.com", "$2a$hash", AccountStatus.ACTIVE, now, now);
        when(userPersistencePort.findById(user.id())).thenReturn(Optional.of(user));
        when(passwordHashingPort.matches("ContraseñaSegura1", "$2a$hash")).thenReturn(true);
    }

    @Test
    void logsInWithUsername() {
        when(accountPersistencePort.findByUsernameOrEmail("brayan.marin"))
                .thenReturn(Optional.of(account));

        LoginResult result = service.login(new LoginCommand(" Brayan.Marin ", "ContraseñaSegura1"));

        assertThat(result.authenticated()).isTrue();
        assertThat(result.user().email()).isEqualTo("brayan@example.com");
        verify(passwordHashingPort).matches("ContraseñaSegura1", "$2a$hash");
    }

    @Test
    void logsInWithEmailCaseInsensitively() {
        when(accountPersistencePort.findByUsernameOrEmail("brayan@example.com"))
                .thenReturn(Optional.of(account));

        LoginResult result = service.login(new LoginCommand("BRAYAN@EXAMPLE.COM", "ContraseñaSegura1"));

        assertThat(result.authenticated()).isTrue();
        verify(accountPersistencePort).findByUsernameOrEmail("brayan@example.com");
    }

    @Test
    void usesGenericErrorForUnknownAccountAndPerformsDummyComparison() {
        when(accountPersistencePort.findByUsernameOrEmail("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginCommand("unknown", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas.");

        verify(passwordHashingPort).matches(eq("wrong"), anyString());
        verifyNoInteractions(userPersistencePort);
    }

    @Test
    void usesSameGenericErrorForWrongPassword() {
        when(accountPersistencePort.findByUsernameOrEmail("brayan.marin"))
                .thenReturn(Optional.of(account));
        when(passwordHashingPort.matches("wrong", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand("brayan.marin", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas.");
    }

    @Test
    void rejectsNonActiveAccountsWithoutRevealingTheirState() {
        Account locked = new Account(account.id(), account.userId(), account.username(),
                account.email(), account.passwordHash(), AccountStatus.LOCKED,
                account.createdAt(), account.updatedAt());
        when(accountPersistencePort.findByUsernameOrEmail("brayan.marin"))
                .thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> service.login(new LoginCommand("brayan.marin", "ContraseñaSegura1")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(passwordHashingPort, never()).matches(anyString(), anyString());
    }

    @Test
    void doesNotExposeCredentialsInCommandOrResult() {
        assertThat(new LoginCommand("brayan.marin", "ContraseñaSegura1").toString())
                .doesNotContain("brayan.marin", "ContraseñaSegura1");
        when(accountPersistencePort.findByUsernameOrEmail("brayan.marin"))
                .thenReturn(Optional.of(account));
        LoginResult result = service.login(new LoginCommand("brayan.marin", "ContraseñaSegura1"));
        assertThat(result.toString()).doesNotContain("password", "hash", "ContraseñaSegura1");
    }
}
