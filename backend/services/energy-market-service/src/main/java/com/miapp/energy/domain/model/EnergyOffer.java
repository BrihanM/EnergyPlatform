package com.miapp.energy.domain.model;

import com.miapp.energy.domain.exception.EnergyOfferDomainException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root for the publication lifecycle of surplus energy.
 */
public final class EnergyOffer {

    public static final int ENERGY_SCALE = 3;
    public static final int PRICE_SCALE = 2;

    private final UUID id;
    private final UUID sellerId;
    private BigDecimal energyAmountKwh;
    private BigDecimal pricePerKwh;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
    private EnergyOfferStatus status;
    private long version;

    private EnergyOffer(UUID id, UUID sellerId, BigDecimal energyAmountKwh,
                        BigDecimal pricePerKwh, Instant createdAt, Instant updatedAt,
                        Instant expiresAt, EnergyOfferStatus status, long version) {
        this.id = require(id, "Offer id is required");
        this.sellerId = require(sellerId, "Seller id is required");
        this.energyAmountKwh = positive(energyAmountKwh, "Energy amount must be greater than zero",
                ENERGY_SCALE);
        this.pricePerKwh = positive(pricePerKwh, "Price per kWh must be greater than zero",
                PRICE_SCALE);
        this.createdAt = require(createdAt, "Creation date is required");
        this.updatedAt = require(updatedAt, "Update date is required");
        this.expiresAt = require(expiresAt, "Expiration date is required");
        this.status = require(status, "Offer status is required");
        this.version = version;
    }

    public static EnergyOffer create(UUID sellerId, BigDecimal energyAmountKwh,
                                     BigDecimal pricePerKwh, Instant expiresAt, Instant now) {
        future(expiresAt, now);
        return new EnergyOffer(UUID.randomUUID(), sellerId, energyAmountKwh, pricePerKwh,
                now, now, expiresAt, EnergyOfferStatus.ACTIVE, 0);
    }

    public static EnergyOffer restore(UUID id, UUID sellerId, BigDecimal energyAmountKwh,
                                      BigDecimal pricePerKwh, Instant createdAt, Instant updatedAt,
                                      Instant expiresAt, EnergyOfferStatus status, long version) {
        return new EnergyOffer(id, sellerId, energyAmountKwh, pricePerKwh, createdAt, updatedAt,
                expiresAt, status, version);
    }

    public void update(BigDecimal newEnergyAmountKwh, BigDecimal newPricePerKwh,
                       Instant newExpiresAt, Instant now) {
        expireIfNeeded(now);
        if (status != EnergyOfferStatus.ACTIVE) {
            throw new EnergyOfferDomainException("Only active offers can be modified");
        }
        energyAmountKwh = positive(newEnergyAmountKwh, "Energy amount must be greater than zero",
                ENERGY_SCALE);
        pricePerKwh = positive(newPricePerKwh, "Price per kWh must be greater than zero",
                PRICE_SCALE);
        expiresAt = future(newExpiresAt, now);
        updatedAt = now;
    }

    public void cancel(Instant now) {
        expireIfNeeded(now);
        if (status != EnergyOfferStatus.ACTIVE) {
            throw new EnergyOfferDomainException("Only active offers can be cancelled");
        }
        status = EnergyOfferStatus.CANCELLED;
        updatedAt = now;
    }

    public void expireIfNeeded(Instant now) {
        if (status == EnergyOfferStatus.ACTIVE && !expiresAt.isAfter(now)) {
            status = EnergyOfferStatus.EXPIRED;
            updatedAt = now;
        }
    }

    private static BigDecimal positive(BigDecimal value, String message, int scale) {
        if (value == null || value.signum() <= 0 || value.scale() > scale) {
            throw new EnergyOfferDomainException(message + " and must use at most " + scale + " decimals");
        }
        return value;
    }

    private static Instant future(Instant value, Instant now) {
        if (value == null || !value.isAfter(now)) {
            throw new EnergyOfferDomainException("Expiration date must be in the future");
        }
        return value;
    }

    private static <T> T require(T value, String message) {
        if (value == null) {
            throw new EnergyOfferDomainException(message);
        }
        return value;
    }

    public UUID id() { return id; }
    public UUID sellerId() { return sellerId; }
    public BigDecimal energyAmountKwh() { return energyAmountKwh; }
    public BigDecimal pricePerKwh() { return pricePerKwh; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant expiresAt() { return expiresAt; }
    public EnergyOfferStatus status() { return status; }
    public long version() { return version; }
}
