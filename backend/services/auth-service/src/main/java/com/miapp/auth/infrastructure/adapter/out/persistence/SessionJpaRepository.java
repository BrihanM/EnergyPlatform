package com.miapp.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data interno al adaptador de sesiones.
 */
interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, UUID> {

    Optional<SessionJpaEntity> findByRefreshTokenHash(String refreshTokenHash);
}
