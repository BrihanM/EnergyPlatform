package com.miapp.auth.infrastructure.config;

import com.miapp.auth.application.command.RegisterUserCommand;
import com.miapp.auth.application.port.in.RegisterUserUseCase;
import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.application.service.AuthAuditService;
import com.miapp.auth.domain.model.AccountStatus;
import com.miapp.auth.domain.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Development-only seed that provisions the demo administrator account.
 *
 * <p>The first ADMIN cannot be created through the HTTP API, so this component
 * registers the demo account through {@link RegisterUserUseCase} and then applies
 * the same single-administrator rule as {@link AdminBootstrap}. It is inert unless
 * {@code dev.seed.enabled=true} and never overwrites an existing account's password.</p>
 */
@Component
@ConditionalOnProperty(name = "dev.seed.enabled", havingValue = "true")
public class DevSeedAdminBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevSeedAdminBootstrap.class);

    private final RegisterUserUseCase registerUser;
    private final AccountPersistencePort accounts;
    private final RolePersistencePort roles;
    private final AuthAuditService audit;
    private final String username;
    private final String email;
    private final String password;

    public DevSeedAdminBootstrap(RegisterUserUseCase registerUser,
                                 AccountPersistencePort accounts,
                                 RolePersistencePort roles,
                                 AuthAuditService audit,
                                 @Value("${dev.seed.admin-username:demo-admin}") String username,
                                 @Value("${dev.seed.admin-email:demo-admin@example.test}") String email,
                                 @Value("${dev.seed.admin-password:}") String password) {
        this.registerUser = registerUser;
        this.accounts = accounts;
        this.roles = roles;
        this.audit = audit;
        this.username = username.trim().toLowerCase(Locale.ROOT);
        this.email = email.trim();
        this.password = password;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedAdministrator() {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("dev.seed.admin-password must be set when dev.seed.enabled=true");
        }
        roles.lockAdministratorInvariant();
        if (roles.countActiveAdministrators() > 0) {
            LOGGER.info("Dev seed: an active administrator already exists; demo admin not promoted");
            return;
        }
        var account = accounts.findByUsername(username)
                .orElseGet(this::registerDemoAccount);
        if (account.status() != AccountStatus.ACTIVE) {
            LOGGER.warn("Dev seed skipped: demo admin account is inactive");
            return;
        }
        if (roles.findRolesByUserId(account.userId()).contains(Role.SECURITY_AUDITOR)) {
            LOGGER.warn("Dev seed skipped: demo admin account already has the auditor role");
            return;
        }
        roles.assignRole(account.userId(), account.userId(), Role.ADMIN);
        audit.recordSystemRoleAssignment(account.userId(), Role.ADMIN);
        LOGGER.info("Dev seed assigned the ADMIN role to the demo administrator account");
    }

    private com.miapp.auth.domain.model.Account registerDemoAccount() {
        registerUser.register(new RegisterUserCommand("Demo", "Admin", username, email, password));
        LOGGER.info("Dev seed registered the demo administrator account");
        return accounts.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Dev seed could not load the registered demo account"));
    }
}
