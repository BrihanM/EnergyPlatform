package com.miapp.energy.infrastructure.adapter.in.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record EnergyOfferRequest(
        @NotNull @DecimalMin(value = "0.001") @Digits(integer = 9, fraction = 3)
        BigDecimal energyAmountKwh,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 12, fraction = 2)
        BigDecimal pricePerKwh,
        @NotNull @Future Instant expiresAt) {
}
