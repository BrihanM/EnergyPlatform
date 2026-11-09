package com.miapp.energy.domain.model;

import com.miapp.energy.domain.exception.EnergyTransactionDomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public final class EnergyTransaction {
    public static final int TOTAL_SCALE = 2;
    public static final String CURRENCY = "COP";

    private final UUID id;
    private final UUID offerId;
    private final UUID sellerId;
    private final UUID buyerId;
    private final BigDecimal energyAmountKwh;
    private final BigDecimal pricePerKwh;
    private final BigDecimal totalAmount;
    private final String currency;
    private final String idempotencyKey;
    private final Instant createdAt;
    private EnergyTransactionStatus status;
    private Instant completedAt;

    private EnergyTransaction(UUID id, UUID offerId, UUID sellerId, UUID buyerId,
                              BigDecimal energyAmountKwh, BigDecimal pricePerKwh,
                              BigDecimal totalAmount, String currency, EnergyTransactionStatus status,
                              String idempotencyKey, Instant createdAt, Instant completedAt) {
        this.id = required(id, "Transaction id is required");
        this.offerId = required(offerId, "Offer id is required");
        this.sellerId = required(sellerId, "Seller id is required");
        this.buyerId = required(buyerId, "Buyer id is required");
        if (sellerId.equals(buyerId)) {
            throw new EnergyTransactionDomainException("Users cannot buy their own offer");
        }
        this.energyAmountKwh = positive(energyAmountKwh, 3, "Energy amount is invalid");
        this.pricePerKwh = positive(pricePerKwh, 2, "Price per kWh is invalid");
        this.totalAmount = this.energyAmountKwh.multiply(this.pricePerKwh)
                .setScale(TOTAL_SCALE, RoundingMode.HALF_UP);
        this.currency = required(currency, "Currency is required");
        this.status = required(status, "Transaction status is required");
        this.idempotencyKey = required(idempotencyKey, "Idempotency key is required");
        this.createdAt = required(createdAt, "Creation date is required");
        this.completedAt = completedAt;
    }

    public static EnergyTransaction pending(UUID offerId, UUID sellerId, UUID buyerId,
                                            BigDecimal energyAmountKwh, BigDecimal pricePerKwh,
                                            String idempotencyKey, Instant now) {
        return new EnergyTransaction(UUID.randomUUID(), offerId, sellerId, buyerId,
                energyAmountKwh, pricePerKwh, null, CURRENCY,
                EnergyTransactionStatus.PENDING_VERIFICATION, idempotencyKey, now, null);
    }

    public static EnergyTransaction restore(UUID id, UUID offerId, UUID sellerId, UUID buyerId,
                                            BigDecimal energyAmountKwh, BigDecimal pricePerKwh,
                                            BigDecimal totalAmount, String currency,
                                            EnergyTransactionStatus status, String idempotencyKey,
                                            Instant createdAt, Instant completedAt) {
        EnergyTransaction transaction = new EnergyTransaction(id, offerId, sellerId, buyerId,
                energyAmountKwh, pricePerKwh, totalAmount, currency, status, idempotencyKey,
                createdAt, completedAt);
        if (totalAmount != null && transaction.totalAmount.compareTo(totalAmount) != 0) {
            throw new EnergyTransactionDomainException("Transaction total is inconsistent");
        }
        return transaction;
    }

    public void complete(boolean accepted, Instant now) {
        if (status != EnergyTransactionStatus.PENDING_VERIFICATION) {
            throw new EnergyTransactionDomainException("Transaction is already finalized");
        }
        status = accepted ? EnergyTransactionStatus.CONFIRMED : EnergyTransactionStatus.REJECTED;
        completedAt = required(now, "Completion date is required");
    }

    private static BigDecimal positive(BigDecimal value, int scale, String message) {
        if (value == null || value.signum() <= 0 || value.scale() > scale) {
            throw new EnergyTransactionDomainException(message);
        }
        return value;
    }

    private static <T> T required(T value, String message) {
        if (value == null) throw new EnergyTransactionDomainException(message);
        return value;
    }

    public UUID id() { return id; }
    public UUID offerId() { return offerId; }
    public UUID sellerId() { return sellerId; }
    public UUID buyerId() { return buyerId; }
    public BigDecimal energyAmountKwh() { return energyAmountKwh; }
    public BigDecimal pricePerKwh() { return pricePerKwh; }
    public BigDecimal totalAmount() { return totalAmount; }
    public String currency() { return currency; }
    public String idempotencyKey() { return idempotencyKey; }
    public Instant createdAt() { return createdAt; }
    public EnergyTransactionStatus status() { return status; }
    public Instant completedAt() { return completedAt; }
}
