package com.miapp.auth.infrastructure.adapter.out.persistence;

import com.miapp.auth.domain.model.User;

/**
 * Traduce entre el modelo de dominio {@link User} y su representación JPA.
 */
final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    static UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(user.id(), user.displayName(), user.createdAt(), user.updatedAt());
    }

    static User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getDisplayName(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
