package com.miapp.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, UserRoleId> {

    List<UserRoleJpaEntity> findByIdUserId(UUID userId);
}
