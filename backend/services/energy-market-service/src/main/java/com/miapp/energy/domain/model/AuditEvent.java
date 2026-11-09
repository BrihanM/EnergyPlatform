package com.miapp.energy.domain.model;

import java.time.Instant;
import java.util.UUID;

/** An allow-listed market fact, without request bodies or credentials. */
public record AuditEvent(UUID id, Type type, Instant occurredAt, ActorType actorType,
                         UUID actorId, UUID affectedUserId, Resource resource, UUID resourceId,
                         Result result) {
    public enum Type {
        OFFER_CREATED, OFFER_UPDATED, OFFER_CANCELLED,
        PURCHASE_STARTED, PURCHASE_CONFIRMED, PURCHASE_REJECTED,
        METER_PROVISIONED, METER_CREDENTIAL_ROTATED, METER_ACTIVATED, METER_SUSPENDED, METER_REVOKED,
        READING_INCONSISTENT
    }
    public enum ActorType { USER, DEVICE }
    public enum Resource { OFFER, TRANSACTION, DEVICE, READING }
    public enum Result { SUCCESS, REJECTED }
}
