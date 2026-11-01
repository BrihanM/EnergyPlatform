package com.miapp.auth.application.port.out;

import com.miapp.auth.domain.model.Account;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para la persistencia de cuentas de acceso.
 */
public interface AccountPersistencePort {

    /**
     * Persiste una cuenta sin exponer detalles de Spring Data al dominio.
     *
     * @param account cuenta que se almacenará
     * @return cuenta persistida
     */
    Account save(Account account);

    /**
     * Busca una cuenta por su identificador de acceso único.
     *
     * @param loginIdentifier identificador utilizado para acceder
     * @return cuenta encontrada, si existe
     */
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<Account> findByUsername(String username);

    /**
     * Busca una cuenta por el identificador normalizado, que puede ser username
     * o email.
     *
     * @param identifier username o email normalizado
     * @return cuenta encontrada, si existe
     */
    Optional<Account> findByUsernameOrEmail(String identifier);

    Optional<Account> findById(UUID id);
}
