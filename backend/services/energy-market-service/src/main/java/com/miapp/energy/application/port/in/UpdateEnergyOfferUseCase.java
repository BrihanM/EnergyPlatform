package com.miapp.energy.application.port.in;

import com.miapp.energy.domain.model.EnergyOffer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface UpdateEnergyOfferUseCase {
    EnergyOffer update(UUID sellerId, UUID offerId, BigDecimal energyAmountKwh,
                       BigDecimal pricePerKwh, Instant expiresAt);
}
