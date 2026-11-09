package com.miapp.auth.infrastructure.adapter.out.persistence;

import com.miapp.auth.application.port.out.UserPersistencePort;
import com.miapp.auth.domain.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia de identidades sobre {@code users}.
 *
 * <p>Los nombres se cifran y descifran en PostgreSQL con pgcrypto. La clave se pasa como
 * parámetro vinculado, nunca concatenada en el SQL. Participa en la transacción activa de JPA
 * porque {@link JdbcTemplate} reutiliza la conexión enlazada por el gestor de transacciones.</p>
 */
@Component
public class UserPersistenceAdapter implements UserPersistencePort {

    private static final String UPSERT_SQL = """
            INSERT INTO users (id, first_name_enc, last_name_enc, created_at, updated_at)
            VALUES (?, pgp_sym_encrypt(?, ?), pgp_sym_encrypt(?, ?), ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                first_name_enc = EXCLUDED.first_name_enc,
                last_name_enc = EXCLUDED.last_name_enc,
                updated_at = EXCLUDED.updated_at
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id,
                   pgp_sym_decrypt(first_name_enc, ?) AS first_name,
                   pgp_sym_decrypt(last_name_enc, ?) AS last_name,
                   created_at,
                   updated_at
            FROM users
            WHERE id = ?
            """;

    private final JdbcTemplate jdbc;
    private final PiiEncryptionKeyProvider keyProvider;

    public UserPersistenceAdapter(JdbcTemplate jdbc, PiiEncryptionKeyProvider keyProvider) {
        this.jdbc = jdbc;
        this.keyProvider = keyProvider;
    }

    @Override
    public User save(User user) {
        String key = keyProvider.key();
        jdbc.update(UPSERT_SQL,
                user.id(),
                user.firstName(), key,
                user.lastName(), key,
                Timestamp.from(user.createdAt()),
                Timestamp.from(user.updatedAt()));
        return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
        String key = keyProvider.key();
        List<User> users = jdbc.query(FIND_BY_ID_SQL,
                (rs, row) -> new User(rs.getObject("id", UUID.class),
                        rs.getString("first_name"), rs.getString("last_name"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()),
                key, key, id);
        return users.stream().findFirst();
    }
}
