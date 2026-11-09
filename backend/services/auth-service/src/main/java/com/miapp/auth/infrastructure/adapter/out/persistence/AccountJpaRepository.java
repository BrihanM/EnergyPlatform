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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from AccountJpaEntity a where a.id = :id")
    Optional<AccountJpaEntity> findByIdForUpdate(java.util.UUID id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query(
            "select a from AccountJpaEntity a where a.username = :identifier or a.email = :identifier")
    Optional<AccountJpaEntity> findByIdentifierForUpdate(String identifier);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from AccountJpaEntity a where a.userId = :userId")
    Optional<AccountJpaEntity> findByUserIdForUpdate(UUID userId);
}
