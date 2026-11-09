package com.miapp.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/** An identity-security fact containing no credentials or request payload. */
public record AuditEvent(UUID id, Type type, Instant occurredAt, ActorType actorType,
                         UUID actorId, UUID affectedUserId, Resource resource, UUID resourceId,
                         Result result, Role role) {
    public enum Type { LOGIN_SUCCEEDED, LOGIN_FAILED, SESSION_REFRESHED, LOGOUT,
        ROLE_ASSIGNED, ROLE_REVOKED, USER_SUSPENDED, USER_REACTIVATED }
    public enum ActorType { USER, ANONYMOUS, SYSTEM }
    public enum Resource { ACCOUNT, SESSION, USER }
    public enum Result { SUCCESS, REJECTED }
}
