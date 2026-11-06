package com.miapp.energy.infrastructure.adapter.in.web;

import com.miapp.energy.domain.model.EnergyOffer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EnergyOfferResponse(UUID id, UUID sellerId, BigDecimal energyAmountKwh,
                                  BigDecimal pricePerKwh, String status, Instant expiresAt,
                                  Instant createdAt, Instant updatedAt) {
    public static EnergyOfferResponse from(EnergyOffer offer) {
        return new EnergyOfferResponse(offer.id(), offer.sellerId(), offer.energyAmountKwh(),
                offer.pricePerKwh(), offer.status().name(), offer.expiresAt(),
                offer.createdAt(), offer.updatedAt());
    }
}
