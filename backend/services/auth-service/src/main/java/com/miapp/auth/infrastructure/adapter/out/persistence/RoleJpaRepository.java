package com.miapp.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, String> {

    interface RoleProjection {
        UUID getUserId();
        String getRoleCode();
    }

    @org.springframework.data.jpa.repository.Query("""
            select r.id.userId as userId, r.id.roleCode as roleCode
            from UserRoleJpaEntity r
            where r.id.userId = :userId
            """)
    java.util.List<RoleProjection> findAssignments(UUID userId);

    @org.springframework.data.jpa.repository.Query("""
            select count(ur)
            from UserRoleJpaEntity ur
            join AccountJpaEntity a on a.userId = ur.id.userId
            where ur.id.roleCode = 'ADMIN' and a.status = com.miapp.auth.domain.model.AccountStatus.ACTIVE
            """)
    long countActiveAdministrators();

    @org.springframework.data.jpa.repository.Query("""
            select case when count(ur) > 0 then true else false end
            from UserRoleJpaEntity ur
            join AccountJpaEntity a on a.userId = ur.id.userId
            where ur.id.userId = :userId and ur.id.roleCode = 'ADMIN'
              and a.status = com.miapp.auth.domain.model.AccountStatus.ACTIVE
            """)
    boolean hasActiveAdmin(UUID userId);
}
