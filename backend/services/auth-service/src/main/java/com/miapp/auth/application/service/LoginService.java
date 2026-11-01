package com.miapp.auth.application.service;

import com.miapp.auth.application.command.LoginCommand;
import com.miapp.auth.application.port.in.LoginUseCase;
import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.application.port.out.PasswordHashingPort;
import com.miapp.auth.application.port.out.UserPersistencePort;
import com.miapp.auth.application.port.out.AccessTokenGeneratorPort;
import com.miapp.auth.application.result.CreateSessionResult;
import com.miapp.auth.application.result.LoginResult;
import com.miapp.auth.domain.exception.InvalidCredentialsException;
import com.miapp.auth.domain.model.Account;
import com.miapp.auth.domain.model.AccountStatus;
import com.miapp.auth.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Clock;

/**
 * Servicio de aplicación para autenticar cuentas mediante username o email.
 *
 * <p>Las cuentas inexistentes, las contraseñas incorrectas y los estados no
 * autenticables comparten el mismo error para evitar enumeración.</p>
 */
@Service
public class LoginService implements LoginUseCase {

    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoO8Y6M4r9p8Q6mT0dXj4n1n7w5o5z5W5W";

    private final AccountPersistencePort accountPersistencePort;
    private final UserPersistencePort userPersistencePort;
    private final PasswordHashingPort passwordHashingPort;
    private final SessionService sessionService;
    private final AccessTokenGeneratorPort accessTokenGenerator;
    private final Clock clock;

    public LoginService(AccountPersistencePort accountPersistencePort,
                        UserPersistencePort userPersistencePort,
                        PasswordHashingPort passwordHashingPort) {
        this(accountPersistencePort, userPersistencePort, passwordHashingPort,
                null, null, null);
    }

    @Autowired
    public LoginService(AccountPersistencePort accountPersistencePort,
                        UserPersistencePort userPersistencePort,
                        PasswordHashingPort passwordHashingPort,
                        SessionService sessionService,
                        AccessTokenGeneratorPort accessTokenGenerator,
                        Clock clock) {
        this.accountPersistencePort = accountPersistencePort;
        this.userPersistencePort = userPersistencePort;
        this.passwordHashingPort = passwordHashingPort;
        this.sessionService = sessionService;
        this.accessTokenGenerator = accessTokenGenerator;
        this.clock = clock;
    }

    @Override
    public LoginResult login(LoginCommand command) {
        validateCommand(command);
        String identifier = normalizeIdentifier(command.identifier());
        Account account = accountPersistencePort.findByUsernameOrEmail(identifier)
                .orElse(null);

        if (account == null) {
            passwordHashingPort.matches(command.password(), DUMMY_PASSWORD_HASH);
            throw new InvalidCredentialsException();
        }
        if (account.status() != AccountStatus.ACTIVE
                || !passwordHashingPort.matches(command.password(), account.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        User user = userPersistencePort.findById(account.userId())
                .orElseThrow(InvalidCredentialsException::new);
        LoginResult.UserSummary summary = new LoginResult.UserSummary(
                user.id(), user.firstName(), user.lastName(),
                account.username(), account.email(), user.createdAt());
        if (sessionService == null) {
            return new LoginResult(true, summary);
        }
        CreateSessionResult session = sessionService.create(account.id());
        AccessTokenGeneratorPort.AccessToken accessToken =
                accessTokenGenerator.generate(user.id(), account.username(), clock.instant());
        return new LoginResult(true, summary, accessToken.value(),
                accessToken.expiresInSeconds(), session.refreshToken());
    }

    /**
     * Reutiliza exactamente la política de normalización del registro.
     *
     * @param identifier username o email recibido
     * @return identificador normalizado
     */
    public static String normalizeIdentifier(String identifier) {
        return RegisterUserService.normalizeUsername(identifier);
    }

    private static void validateCommand(LoginCommand command) {
        if (command == null || command.identifier() == null
                || command.identifier().isBlank()
                || command.password() == null || command.password().isBlank()) {
            throw new InvalidCredentialsException();
        }
    }
}
