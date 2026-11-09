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
    public JdbcUserAdministrationAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<UserRow> findUsers(int limit, long offset) {
        return jdbc.query("""
                SELECT u.id, u.first_name, u.last_name, a.username, a.email, a.status, u.created_at
                FROM users u JOIN accounts a ON a.user_id = u.id
                ORDER BY u.created_at DESC, u.id DESC LIMIT ? OFFSET ?
                """, (rs, row) -> new UserRow(rs.getObject("id", UUID.class), rs.getString("first_name"),
                rs.getString("last_name"), rs.getString("username"), rs.getString("email"),
                AccountStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant()),
                limit, offset);
    }
}
