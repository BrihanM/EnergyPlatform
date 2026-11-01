package com.miapp.auth.infrastructure.adapter.out.persistence;

import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.domain.model.Account;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador que implementa el puerto de cuentas mediante Spring Data JPA.
 */
@Component
public class AccountPersistenceAdapter implements AccountPersistencePort {

    private final AccountJpaRepository repository;

    public AccountPersistenceAdapter(AccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Account save(Account account) {
        return AccountPersistenceMapper.toDomain(repository.save(AccountPersistenceMapper.toEntity(account)));
    }

    @Override
    public Optional<Account> findByLoginIdentifier(String loginIdentifier) {
        return repository.findByLoginIdentifier(loginIdentifier)
                .map(AccountPersistenceMapper::toDomain);
    }
}
