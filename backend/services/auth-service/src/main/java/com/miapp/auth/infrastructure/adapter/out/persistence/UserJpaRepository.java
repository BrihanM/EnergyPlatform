package com.miapp.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio Spring Data interno al adaptador de persistencia de usuarios.
 */
interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
}
