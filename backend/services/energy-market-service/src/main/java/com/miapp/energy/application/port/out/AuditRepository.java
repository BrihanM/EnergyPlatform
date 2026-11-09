package com.miapp.energy.application.port.out;

import com.miapp.energy.domain.model.AuditEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditRepository {
    void append(AuditEvent event);
    List<AuditEvent> find(Instant from, Instant to, UUID user, AuditEvent.Type type, int limit, long offset);
}
