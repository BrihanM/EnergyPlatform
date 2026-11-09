package com.miapp.auth.infrastructure.adapter.out.persistence;

import com.miapp.auth.application.port.out.UserAdministrationPort;
import com.miapp.auth.domain.model.AccountStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JdbcUserAdministrationAdapter implements UserAdministrationPort {
    private final JdbcTemplate jdbc;
    private final PiiEncryptionKeyProvider keyProvider;

    public JdbcUserAdministrationAdapter(JdbcTemplate jdbc, PiiEncryptionKeyProvider keyProvider) {
        this.jdbc = jdbc;
        this.keyProvider = keyProvider;
    }

    /**
     * Lista usuarios para administración. Los nombres se descifran solo aquí, en la consulta autorizada.
     */
    @Override
    public List<UserRow> findUsers(int limit, long offset) {
        String key = keyProvider.key();
        return jdbc.query("""
                SELECT u.id,
                       pgp_sym_decrypt(u.first_name_enc, ?) AS first_name,
                       pgp_sym_decrypt(u.last_name_enc, ?) AS last_name,
                       a.username, a.email, a.status, u.created_at
                FROM users u JOIN accounts a ON a.user_id = u.id
                ORDER BY u.created_at DESC, u.id DESC LIMIT ? OFFSET ?
                """, (rs, row) -> new UserRow(rs.getObject("id", UUID.class), rs.getString("first_name"),
                rs.getString("last_name"), rs.getString("username"), rs.getString("email"),
                AccountStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant()),
                key, key, limit, offset);
    }
}
