package com.miapp.auth.application.port.out;

import com.miapp.auth.domain.model.AuditEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditPersistencePort {
    void append(AuditEvent event);
    List<AuditEvent> find(Instant from, Instant to, UUID user, AuditEvent.Type type, int limit, long offset);
}
