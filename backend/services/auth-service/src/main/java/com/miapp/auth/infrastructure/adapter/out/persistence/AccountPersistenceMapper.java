package com.miapp.auth.infrastructure.adapter.out.persistence;

import com.miapp.auth.domain.model.Account;

/**
 * Traduce entre el agregado de cuenta y la entidad técnica de persistencia.
 */
final class AccountPersistenceMapper {

    private AccountPersistenceMapper() {
    }

    static AccountJpaEntity toEntity(Account account) {
        return new AccountJpaEntity(account.id(), account.userId(), account.loginIdentifier(),
                account.passwordHash(), account.status(), account.createdAt(), account.updatedAt());
    }

    static Account toDomain(AccountJpaEntity entity) {
        return new Account(entity.getId(), entity.getUserId(), entity.getLoginIdentifier(),
                entity.getPasswordHash(), entity.getStatus(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
