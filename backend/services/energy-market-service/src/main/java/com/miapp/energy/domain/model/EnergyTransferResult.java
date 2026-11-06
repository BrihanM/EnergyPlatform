package com.miapp.energy.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EnergyTransferResult(
        boolean successful,
        BigDecimal energyAmountKwh,
        UUID simulationReference,
        Instant executedAt,
        String reason) {
}
