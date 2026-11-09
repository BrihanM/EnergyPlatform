package com.miapp.energy.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A meter has a technical identity distinct from its owner's user identity. */
public record MeterDevice(UUID id, UUID ownerUserId, String deviceIdentifier,
                          Status status, Instant registeredAt) {
    public enum Status { ACTIVE, SUSPENDED, REVOKED }

    public MeterDevice {
        Objects.requireNonNull(id);
        Objects.requireNonNull(ownerUserId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(registeredAt);
        if (deviceIdentifier == null || !deviceIdentifier.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new IllegalArgumentException("Invalid device identifier");
        }
    }

    public MeterDevice changeStatus(Status next) {
        Objects.requireNonNull(next);
        if (status == Status.REVOKED && next != Status.REVOKED) {
            throw new IllegalStateException("Revoked devices cannot be reactivated");
        }
        return new MeterDevice(id, ownerUserId, deviceIdentifier, next, registeredAt);
    }
}
