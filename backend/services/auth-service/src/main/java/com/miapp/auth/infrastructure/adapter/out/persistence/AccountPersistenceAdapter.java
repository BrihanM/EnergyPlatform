package com.miapp.auth.infrastructure.adapter.out.persistence;

import com.miapp.auth.application.port.out.AccountPersistencePort;
import com.miapp.auth.domain.model.Account;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

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
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        return repository.findByUsername(username)
                .map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Account> findByUsernameOrEmail(String identifier) {
        return repository.findByUsernameOrEmail(identifier, identifier)
                .map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return repository.findById(id).map(AccountPersistenceMapper::toDomain);
    }
}
