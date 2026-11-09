package com.miapp.auth.infrastructure.config;

import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.domain.model.AccountStatus;
import com.miapp.auth.domain.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotently assigns the first administrator to an existing account selected
 * by configuration. It never creates or replaces credentials.
 */
@Component
public class AdminBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AccountPersistencePort accounts;
    private final RolePersistencePort roles;
    private final String username;

    public AdminBootstrap(AccountPersistencePort accounts,
                          RolePersistencePort roles,
                          @Value("${security.rbac.bootstrap-admin-username:}") String username) {
        this.accounts = accounts;
        this.roles = roles;
        this.username = username == null ? "" : username.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrap() {
        if (username.isBlank() || roles.countActiveAdministrators() > 0) {
            return;
        }
        var account = accounts.findByUsername(username);
        if (account.isEmpty() || account.get().status() != AccountStatus.ACTIVE) {
            LOGGER.warn("RBAC bootstrap skipped: configured account is absent or inactive");
            return;
        }
        roles.assignRole(account.get().userId(), account.get().userId(), Role.ADMIN);
        LOGGER.info("RBAC bootstrap assigned the initial administrator role to the configured account");
    }
}
