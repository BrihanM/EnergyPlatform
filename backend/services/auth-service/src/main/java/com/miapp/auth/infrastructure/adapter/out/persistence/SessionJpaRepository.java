package com.miapp.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data interno al adaptador de sesiones.
 */
interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessionJpaEntity s where s.id = :id")
    Optional<SessionJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<SessionJpaEntity> findByRefreshTokenHash(String refreshTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessionJpaEntity s where s.refreshTokenHash = :hash")
    Optional<SessionJpaEntity> findByRefreshTokenHashForUpdate(@Param("hash") String hash);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update SessionJpaEntity s set s.revokedAt = :now, s.updatedAt = :now "
            + "where s.accountId = :accountId and s.revokedAt is null")
    int revokeAllForAccount(@Param("accountId") UUID accountId, @Param("now") java.time.Instant now);
}
