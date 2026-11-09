package com.miapp.auth.infrastructure.adapter.out.persistence;

import com.miapp.auth.application.port.out.AuditPersistencePort;
import com.miapp.auth.domain.model.AuditEvent;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcAuthAuditAdapter implements AuditPersistencePort {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcAuthAuditAdapter(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void append(AuditEvent event) {
        jdbc.update("""
                INSERT INTO auth_audit_events
                (id,event_type,occurred_at,actor_type,actor_id,affected_user_id,resource_type,resource_id,result,role_code)
                VALUES (:id,:type,:at,:actorType,:actor,:affected,:resource,:resourceId,:result,:role)
                """, new MapSqlParameterSource().addValue("id", event.id()).addValue("type", event.type().name())
                .addValue("at", Timestamp.from(event.occurredAt())).addValue("actorType", event.actorType().name())
                .addValue("actor", event.actorId()).addValue("affected", event.affectedUserId())
                .addValue("resource", event.resource().name()).addValue("resourceId", event.resourceId())
                .addValue("result", event.result().name())
                .addValue("role", event.role() == null ? null : event.role().name()));
    }

    @Override
    public List<AuditEvent> find(Instant from, Instant to, UUID user, AuditEvent.Type type, int limit, long offset) {
        StringBuilder sql = new StringBuilder("SELECT * FROM auth_audit_events WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", limit).addValue("offset", offset);
        if (from != null) { sql.append(" AND occurred_at >= :from"); params.addValue("from", Timestamp.from(from)); }
        if (to != null) { sql.append(" AND occurred_at <= :to"); params.addValue("to", Timestamp.from(to)); }
        if (user != null) { sql.append(" AND (actor_id = :user OR affected_user_id = :user)"); params.addValue("user", user); }
        if (type != null) { sql.append(" AND event_type = :type"); params.addValue("type", type.name()); }
        sql.append(" ORDER BY occurred_at DESC, id DESC LIMIT :limit OFFSET :offset");
        return jdbc.query(sql.toString(), params, (rs, row) -> new AuditEvent(
                rs.getObject("id", UUID.class), AuditEvent.Type.valueOf(rs.getString("event_type")),
                rs.getTimestamp("occurred_at").toInstant(), AuditEvent.ActorType.valueOf(rs.getString("actor_type")),
                rs.getObject("actor_id", UUID.class), rs.getObject("affected_user_id", UUID.class),
                AuditEvent.Resource.valueOf(rs.getString("resource_type")), rs.getObject("resource_id", UUID.class),
                AuditEvent.Result.valueOf(rs.getString("result")), rs.getString("role_code") == null ? null
                        : com.miapp.auth.domain.model.Role.valueOf(rs.getString("role_code"))));
    }
}
