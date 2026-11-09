package com.miapp.auth.application.service;

import com.miapp.auth.application.port.out.AuditPersistencePort;
import com.miapp.auth.domain.model.AuditEvent;
import com.miapp.auth.domain.model.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthAuditService {
    private final AuditPersistencePort repository;
    public AuthAuditService(AuditPersistencePort repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEvent.Type type, UUID actor, UUID affected, AuditEvent.Resource resource,
                       UUID resourceId) {
        repository.append(new AuditEvent(UUID.randomUUID(), type, Instant.now(), AuditEvent.ActorType.USER,
                actor, affected, resource, resourceId, AuditEvent.Result.SUCCESS, null));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordRole(AuditEvent.Type type, UUID actor, UUID target, Role role) {
        repository.append(new AuditEvent(UUID.randomUUID(), type, Instant.now(), AuditEvent.ActorType.USER,
                actor, target, AuditEvent.Resource.USER, target, AuditEvent.Result.SUCCESS, role));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSystemRoleAssignment(UUID target, Role role) {
        repository.append(new AuditEvent(UUID.randomUUID(), AuditEvent.Type.ROLE_ASSIGNED, Instant.now(),
                AuditEvent.ActorType.SYSTEM, null, target, AuditEvent.Resource.USER, target,
                AuditEvent.Result.SUCCESS, role));
    }

    /** Called after the failed authentication transaction has released its connection. */
    @Transactional
    public void failedLogin() {
        repository.append(new AuditEvent(UUID.randomUUID(), AuditEvent.Type.LOGIN_FAILED, Instant.now(),
                AuditEvent.ActorType.ANONYMOUS, null, null, AuditEvent.Resource.ACCOUNT, null,
                AuditEvent.Result.REJECTED, null));
    }

    @Transactional(readOnly = true)
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
                           UUID resourceId, AuditEvent.Result result, Role role) {
        static EventDto from(AuditEvent event) {
            return new EventDto(event.id(), event.type(), event.occurredAt(), event.actorType(), event.actorId(),
                    event.affectedUserId(), "auth", event.resource(), event.resourceId(), event.result(), event.role());
        }
    }
    public record Page(List<EventDto> items, int page, int size, boolean hasNext) {}
}
