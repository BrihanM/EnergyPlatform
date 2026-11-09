package com.miapp.energy.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable submitted measurement; its validation status is not proof of physical delivery. */
public record EnergyReading(UUID id, UUID meterDeviceId, BigDecimal generatedEnergyKwh,
                            BigDecimal surplusEnergyKwh, Instant measuredAt, Instant receivedAt,
                            Status validationStatus) {
    public enum Status { PENDING, VALID, INCONSISTENT, REJECTED }

    public EnergyReading {
        Objects.requireNonNull(id);
        Objects.requireNonNull(meterDeviceId);
        Objects.requireNonNull(measuredAt);
        Objects.requireNonNull(receivedAt);
        Objects.requireNonNull(validationStatus);
        if (!representable(generatedEnergyKwh) || !representable(surplusEnergyKwh)
                || measuredAt.isAfter(receivedAt)
                || (validationStatus == Status.VALID && surplusEnergyKwh.compareTo(generatedEnergyKwh) > 0)) {
            throw new IllegalArgumentException("Invalid reading");
        }
    }

    public static EnergyReading classify(UUID id, UUID deviceId, BigDecimal generated,
                                         BigDecimal surplus, Instant measured, Instant received) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(measured);
        Objects.requireNonNull(received);
        if (!representable(generated) || !representable(surplus) || measured.isAfter(received)) {
            throw new IllegalArgumentException("Invalid reading");
        }
        Status status = surplus.compareTo(generated) > 0 ? Status.INCONSISTENT : Status.VALID;
        return new EnergyReading(id, deviceId, generated, surplus, measured, received, status);
    }

    private static boolean representable(BigDecimal value) {
        return value != null && value.signum() >= 0 && value.scale() <= 6
                && (long) value.precision() - value.scale() <= 12;
    }
}
