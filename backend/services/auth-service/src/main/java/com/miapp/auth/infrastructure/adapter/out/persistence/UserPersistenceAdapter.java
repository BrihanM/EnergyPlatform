package com.miapp.auth.infrastructure.adapter.out.persistence;

import com.miapp.auth.application.port.out.UserPersistencePort;
import com.miapp.auth.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador que implementa el puerto de persistencia de identidades con JPA.
 */
@Component
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserJpaRepository repository;

    public UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        return UserPersistenceMapper.toDomain(repository.save(UserPersistenceMapper.toEntity(user)));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(UserPersistenceMapper::toDomain);
    }
}
