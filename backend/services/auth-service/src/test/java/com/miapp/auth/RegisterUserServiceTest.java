package com.miapp.auth;

import com.miapp.auth.application.command.RegisterUserCommand;
import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.application.port.out.PasswordHashingPort;
import com.miapp.auth.application.port.out.UserPersistencePort;
import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.application.result.RegisterUserResult;
import com.miapp.auth.application.service.RegisterUserService;
import com.miapp.auth.domain.exception.DuplicateEmailException;
import com.miapp.auth.domain.exception.DuplicateUsernameException;
import com.miapp.auth.domain.exception.InvalidRegistrationException;
import com.miapp.auth.domain.model.Account;
import com.miapp.auth.domain.model.User;
import com.miapp.auth.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del flujo de registro y sus reglas de aplicación.
 */
class RegisterUserServiceTest {

    private UserPersistencePort userPersistencePort;
    private AccountPersistencePort accountPersistencePort;
    private PasswordHashingPort passwordHashingPort;
    private RolePersistencePort rolePersistencePort;
    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        userPersistencePort = mock(UserPersistencePort.class);
        accountPersistencePort = mock(AccountPersistencePort.class);
        passwordHashingPort = mock(PasswordHashingPort.class);
        rolePersistencePort = mock(RolePersistencePort.class);
        service = new RegisterUserService(userPersistencePort, accountPersistencePort,
                passwordHashingPort, rolePersistencePort);
        when(passwordHashingPort.hash("ContraseñaSegura1")).thenReturn("$2a$hash");
        when(userPersistencePort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountPersistencePort.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registersUserAndAccountWithNormalizedIdentifiersAndHash() {
        RegisterUserCommand command = new RegisterUserCommand(
                " Brayan ", " Marín ", "Brayan.Marin", "Brayan@Example.com",
                "ContraseñaSegura1");

        RegisterUserResult result = service.register(command);

        assertThat(result.username()).isEqualTo("brayan.marin");
        assertThat(result.email()).isEqualTo("brayan@example.com");
        assertThat(result.toString()).doesNotContain("password", "hash");
        verify(userPersistencePort).save(any(User.class));
        verify(accountPersistencePort).save(argThat(account ->
                account.username().equals("brayan.marin")
                        && account.email().equals("brayan@example.com")
                        && account.passwordHash().equals("$2a$hash")));
        verify(rolePersistencePort).assignRole(any(), any(), eq(Role.NORMAL_USER));
    }

    @Test
    void rejectsDuplicateUsernameBeforeCreatingAnything() {
        when(accountPersistencePort.existsByUsername("brayan")).thenReturn(true);

        assertThatThrownBy(() -> service.register(validCommand("Brayan", "other@example.com")))
                .isInstanceOf(DuplicateUsernameException.class);

        verify(userPersistencePort, never()).save(any());
        verify(accountPersistencePort, never()).save(any());
        verifyNoInteractions(passwordHashingPort);
    }

    @Test
    void rejectsDuplicateEmailBeforeCreatingAnything() {
        when(accountPersistencePort.existsByEmail("brayan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(validCommand("other", "Brayan@Example.com")))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userPersistencePort, never()).save(any());
        verify(accountPersistencePort, never()).save(any());
    }

    @Test
    void rejectsWeakPassword() {
        assertThatThrownBy(() -> service.register(new RegisterUserCommand(
                "Brayan", "Marín", "brayan", "brayan@example.com", "short")))
                .isInstanceOf(InvalidRegistrationException.class);

        verifyNoInteractions(userPersistencePort, accountPersistencePort, passwordHashingPort);
    }

    @Test
    void commandDoesNotExposePasswordInToString() {
        assertThat(validCommand("brayan", "brayan@example.com").toString())
                .doesNotContain("ContraseñaSegura1");
    }

    private static RegisterUserCommand validCommand(String username, String email) {
        return new RegisterUserCommand("Brayan", "Marín", username, email, "ContraseñaSegura1");
    }
}
