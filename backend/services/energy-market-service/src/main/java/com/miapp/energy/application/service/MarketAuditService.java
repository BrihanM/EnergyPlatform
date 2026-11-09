package com.miapp.energy.application.service;

import com.miapp.energy.application.port.out.AuditRepository;
import com.miapp.energy.domain.model.AuditEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MarketAuditService {
    private final AuditRepository repository;

    public MarketAuditService(AuditRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEvent.Type type, AuditEvent.ActorType actorType, UUID actor, UUID affected,
                       AuditEvent.Resource resource, UUID resourceId, AuditEvent.Result result) {
        repository.append(new AuditEvent(UUID.randomUUID(), type, Instant.now(), actorType, actor,
                affected, resource, resourceId, result));
    }

    public Page query(Instant from, Instant to, UUID user, AuditEvent.Type type, int page, int size) {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 100
                || from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Invalid audit filters");
        }
        List<AuditEvent> rows = repository.find(from, to, user, type, size + 1, (long) page * size);
        boolean hasNext = rows.size() > size;
        return new Page((hasNext ? rows.subList(0, size) : rows).stream().map(EventDto::from).toList(),
                page, size, hasNext);
    }

    public record EventDto(UUID id, AuditEvent.Type type, Instant occurredAt, AuditEvent.ActorType actorType,
                           UUID actorId, UUID affectedUserId, String service, AuditEvent.Resource resource,
                           UUID resourceId, AuditEvent.Result result, String role) {
        static EventDto from(AuditEvent event) {
            return new EventDto(event.id(), event.type(), event.occurredAt(), event.actorType(), event.actorId(),
                    event.affectedUserId(), "energy-market", event.resource(), event.resourceId(), event.result(), null);
        }
    }
    public record Page(List<EventDto> items, int page, int size, boolean hasNext) {}
}
