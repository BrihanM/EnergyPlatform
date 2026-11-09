package com.miapp.energy.application.port.in;

import com.miapp.energy.domain.model.EnergyOffer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface CreateEnergyOfferUseCase {
    EnergyOffer create(UUID sellerId, BigDecimal energyAmountKwh, BigDecimal pricePerKwh,
                       Instant expiresAt);
}
