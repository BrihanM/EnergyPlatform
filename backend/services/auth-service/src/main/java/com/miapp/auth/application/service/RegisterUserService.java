package com.miapp.auth.application.service;

import com.miapp.auth.application.command.RegisterUserCommand;
import com.miapp.auth.application.port.in.RegisterUserUseCase;
import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.application.port.out.PasswordHashingPort;
import com.miapp.auth.application.port.out.UserPersistencePort;
import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.application.result.RegisterUserResult;
import com.miapp.auth.domain.exception.DuplicateEmailException;
import com.miapp.auth.domain.exception.DuplicateUsernameException;
import com.miapp.auth.domain.exception.InvalidRegistrationException;
import com.miapp.auth.domain.exception.RegistrationConflictException;
import com.miapp.auth.domain.model.Account;
import com.miapp.auth.domain.model.AccountStatus;
import com.miapp.auth.domain.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Locale;

/**
 * Servicio de aplicación que coordina el registro transaccional.
 *
 * <p>La base de datos mantiene las restricciones únicas como última barrera
 * frente a condiciones de carrera entre solicitudes concurrentes.</p>
 */
@Service
public class RegisterUserService implements RegisterUserUseCase {

    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final UserPersistencePort userPersistencePort;
    private final AccountPersistencePort accountPersistencePort;
    private final PasswordHashingPort passwordHashingPort;
    private final RolePersistencePort rolePersistencePort;

    public RegisterUserService(UserPersistencePort userPersistencePort,
                               AccountPersistencePort accountPersistencePort,
                               PasswordHashingPort passwordHashingPort) {
        this(userPersistencePort, accountPersistencePort, passwordHashingPort, null);
    }

    @Autowired
    public RegisterUserService(UserPersistencePort userPersistencePort,
                               AccountPersistencePort accountPersistencePort,
                               PasswordHashingPort passwordHashingPort,
                               RolePersistencePort rolePersistencePort) {
        this.userPersistencePort = userPersistencePort;
        this.accountPersistencePort = accountPersistencePort;
        this.passwordHashingPort = passwordHashingPort;
        this.rolePersistencePort = rolePersistencePort;
    }

    @Override
    @Transactional
    public RegisterUserResult register(RegisterUserCommand command) {
        validateCommand(command);
        String username = normalizeUsername(command.username());
        String email = normalizeEmail(command.email());

        if (accountPersistencePort.existsByUsername(username)) {
            throw new DuplicateUsernameException();
        }
        if (accountPersistencePort.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        User user = User.create(command.firstName().trim(), command.lastName().trim());
        Account account = new Account(
                java.util.UUID.randomUUID(),
                user.id(),
                username,
                email,
                passwordHashingPort.hash(command.password()),
                AccountStatus.ACTIVE,
                user.createdAt(),
                user.updatedAt());

        try {
            userPersistencePort.save(user);
            Account savedAccount = accountPersistencePort.save(account);
            if (rolePersistencePort != null) {
                rolePersistencePort.assignRole(user.id(), user.id(), com.miapp.auth.domain.model.Role.NORMAL_USER);
            }
            return new RegisterUserResult(user.id(), user.firstName(), user.lastName(),
                    savedAccount.username(), savedAccount.email(), savedAccount.status(),
                    savedAccount.createdAt());
        } catch (DataIntegrityViolationException exception) {
            // No se consulta la misma transacción: PostgreSQL la marca abortada
            // después de la violación y la siguiente consulta también fallaría.
            throw new RegistrationConflictException();
        }
    }

    /**
     * Normaliza el nombre de usuario a minúsculas con Locale.ROOT para que la
     * comparación Java y la persistencia representen exactamente el mismo valor.
     */
    public static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Normaliza el correo sin alterar su parte local más allá de la política
     * explícita de comparación case-insensitive del servicio.
     */
    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateCommand(RegisterUserCommand command) {
        if (command == null || isBlank(command.firstName()) || isBlank(command.lastName())) {
            throw new InvalidRegistrationException("First name and last name are required");
        }
        if (isBlank(command.username()) || !command.username().trim().matches("[A-Za-z0-9][A-Za-z0-9._-]{1,48}[A-Za-z0-9]")) {
            throw new InvalidRegistrationException("Username must contain 3 to 50 valid characters");
        }
        if (isBlank(command.email()) || !command.email().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new InvalidRegistrationException("Email format is invalid");
        }
        String password = command.password();
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH
                || !password.matches(".*\\p{L}.*")
                || !password.matches(".*\\d.*")) {
            throw new InvalidRegistrationException(
                    "Password must contain 12 to 128 characters, including a letter and a number");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
