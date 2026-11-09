package com.miapp.auth.infrastructure.adapter.out.persistence;

import com.miapp.auth.application.port.out.RolePersistencePort;
import com.miapp.auth.domain.model.Role;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
class RolePersistenceAdapter implements RolePersistencePort {

    private final RoleJpaRepository repository;
    private final UserRoleJpaRepository userRoleRepository;

    RolePersistenceAdapter(RoleJpaRepository repository, UserRoleJpaRepository userRoleRepository) {
        this.repository = repository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Role> findRolesByUserId(UUID userId) {
        return userRoleRepository.findByIdUserId(userId).stream()
                .map(value -> Role.valueOf(value.getId().getRoleCode()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Role> findAllRoles() {
        return repository.findAll().stream()
                .map(value -> Role.valueOf(value.getCode()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
    }

    @Override
    @Transactional
    public void assignRole(UUID actorUserId, UUID targetUserId, Role role) {
        userRoleRepository.save(new UserRoleJpaEntity(
                new UserRoleId(targetUserId, role.name()), Instant.now()));
    }

    @Override
    @Transactional
    public void revokeRole(UUID actorUserId, UUID targetUserId, Role role) {
        userRoleRepository.deleteById(new UserRoleId(targetUserId, role.name()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAdminRole(UUID userId) {
        return repository.hasActiveAdmin(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveAdministrators() {
        return repository.countActiveAdministrators();
    }

    @Override
    @Transactional
    public void lockAdministratorInvariant() {
        repository.lockAdministratorInvariant().orElseThrow(
                () -> new IllegalStateException("Administrator role is missing"));
    }
}
