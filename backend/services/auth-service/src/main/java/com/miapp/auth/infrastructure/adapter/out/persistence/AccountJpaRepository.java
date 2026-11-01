package com.miapp.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data interno al adaptador de persistencia de cuentas.
 */
interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, UUID> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<AccountJpaEntity> findByUsername(String username);

    Optional<AccountJpaEntity> findByUsernameOrEmail(String username, String email);
}
