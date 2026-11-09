package com.miapp.energy.application.service;

import com.miapp.energy.domain.model.EnergyOfferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OfferQuery(BigDecimal minEnergy, BigDecimal maxEnergy, BigDecimal minPrice,
                         BigDecimal maxPrice, Instant expiresBefore, UUID id,
                         EnergyOfferStatus status, int page, int size, String sort) {

    public OfferQuery {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }
        if (minEnergy != null && maxEnergy != null && minEnergy.compareTo(maxEnergy) > 0
                || minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Filter range is invalid");
        }
        if (!"createdAt".equals(sort) && !"expiresAt".equals(sort)
                && !"pricePerKwh".equals(sort) && !"energyAmountKwh".equals(sort)) {
            throw new IllegalArgumentException("Sort field is not allowed");
        }
    }

    public static OfferQuery available(int page, int size, String sort, BigDecimal minEnergy,
                                       BigDecimal maxEnergy, BigDecimal minPrice,
                                       BigDecimal maxPrice, Instant expiresBefore, UUID id) {
        return new OfferQuery(minEnergy, maxEnergy, minPrice, maxPrice, expiresBefore, id,
                EnergyOfferStatus.ACTIVE, page, size, sort);
    }
}
