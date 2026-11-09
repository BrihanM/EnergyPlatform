package com.miapp.energy.application.service;

import com.miapp.energy.application.port.out.EnergyOfferRepository;
import com.miapp.energy.application.port.out.EnergyTransactionRepository;
import com.miapp.energy.application.port.out.EnergyTransferSimulator;
import com.miapp.energy.domain.exception.EnergyOfferDomainException;
import com.miapp.energy.domain.exception.EnergyTransactionDomainException;
import com.miapp.energy.domain.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EnergyTransactionService {
    private final EnergyOfferRepository offers;
    private final EnergyTransactionRepository transactions;
    private final EnergyTransferSimulator simulator;
    private final Clock clock;
    private final MarketAuditService audit;

    @Autowired
    public EnergyTransactionService(EnergyOfferRepository offers, EnergyTransactionRepository transactions,
                                    EnergyTransferSimulator simulator, MarketAuditService audit) {
        this(offers, transactions, simulator, Clock.systemUTC(), audit);
    }

    EnergyTransactionService(EnergyOfferRepository offers, EnergyTransactionRepository transactions,
                             EnergyTransferSimulator simulator, Clock clock, MarketAuditService audit) {
        this.offers = offers;
        this.transactions = transactions;
        this.simulator = simulator;
        this.clock = clock;
        this.audit = audit;
    }

    @Transactional
    public EnergyTransaction purchase(UUID buyerId, UUID offerId, String idempotencyKey) {
        validateKey(idempotencyKey);
        EnergyTransaction existing = transactions.findByBuyerAndKey(buyerId, idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.offerId().equals(offerId)) {
                throw new EnergyTransactionDomainException("Idempotency key was used for another offer");
            }
            return existing;
        }
        EnergyOffer offer = offers.findForPurchase(offerId);
        Instant now = Instant.now(clock);
        offer.expireIfNeeded(now);
        if (offer.status() == EnergyOfferStatus.EXPIRED) {
            offers.setPurchaseStatus(offer.id(), EnergyOfferStatus.EXPIRED);
            throw new EnergyOfferDomainException("Offer is expired");
        }
        if (offer.status() != EnergyOfferStatus.ACTIVE) {
            throw new EnergyOfferDomainException("Offer is no longer available");
        }
        if (offer.sellerId().equals(buyerId)) {
            throw new EnergyTransactionDomainException("Users cannot buy their own offer");
        }
        if (!transactions.claimIdempotency(buyerId, idempotencyKey, offerId)) {
            EnergyTransaction retry = transactions.findByBuyerAndKey(buyerId, idempotencyKey)
                    .orElseThrow(() -> new EnergyTransactionDomainException("Idempotency conflict"));
            if (!retry.offerId().equals(offerId)) {
                throw new EnergyTransactionDomainException("Idempotency key was used for another offer");
            }
            return retry;
        }
        offers.setPurchaseStatus(offer.id(), EnergyOfferStatus.RESERVED);
        EnergyTransaction transaction = transactions.save(EnergyTransaction.pending(offer.id(),
                offer.sellerId(), buyerId, offer.energyAmountKwh(), offer.pricePerKwh(),
                idempotencyKey, now));
        audit.record(com.miapp.energy.domain.model.AuditEvent.Type.PURCHASE_STARTED,
                com.miapp.energy.domain.model.AuditEvent.ActorType.USER, buyerId, buyerId,
                com.miapp.energy.domain.model.AuditEvent.Resource.TRANSACTION, transaction.id(),
                com.miapp.energy.domain.model.AuditEvent.Result.SUCCESS);
        EnergyTransferResult transfer = simulator.transfer(offer);
        transaction.complete(transfer.successful(), transfer.executedAt());
        transactions.saveVerification(transaction.id(),
                transfer.successful() ? VerificationResult.ACCEPTED : VerificationResult.REJECTED,
                transfer.reason(), transfer.simulationReference(), transfer.executedAt());
        offers.setPurchaseStatus(offer.id(), transfer.successful()
                ? EnergyOfferStatus.SOLD
                : (offer.expiresAt().isAfter(transfer.executedAt())
                ? EnergyOfferStatus.ACTIVE : EnergyOfferStatus.EXPIRED));
        EnergyTransaction completed = transactions.update(transaction);
        audit.record(transfer.successful() ? com.miapp.energy.domain.model.AuditEvent.Type.PURCHASE_CONFIRMED
                        : com.miapp.energy.domain.model.AuditEvent.Type.PURCHASE_REJECTED,
                com.miapp.energy.domain.model.AuditEvent.ActorType.USER, buyerId, buyerId,
                com.miapp.energy.domain.model.AuditEvent.Resource.TRANSACTION, transaction.id(),
                transfer.successful() ? com.miapp.energy.domain.model.AuditEvent.Result.SUCCESS
                        : com.miapp.energy.domain.model.AuditEvent.Result.REJECTED);
        return completed;
    }

    @Transactional(readOnly = true)
    public EnergyTransaction get(UUID userId, UUID id) {
        EnergyTransaction transaction = transactions.findById(id)
                .orElseThrow(() -> new EnergyTransactionDomainException("Transaction was not found"));
        if (!transaction.buyerId().equals(userId) && !transaction.sellerId().equals(userId)) {
            throw new EnergyTransactionDomainException("Transaction was not found");
        }
        return transaction;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<EnergyTransactionRepository.VerificationSnapshot> verification(UUID id) {
        return transactions.findVerification(id);
    }

    @Transactional(readOnly = true)
    public List<EnergyTransaction> mine(UUID userId, int limit, int offset,
                                        EnergyTransactionRepository.EnergyTransactionStatusFilter status,
                                        Instant from, Instant to) {
        if (limit < 1 || limit > 100 || offset < 0) {
            throw new EnergyTransactionDomainException("Pagination is invalid");
        }
        return transactions.findForUser(userId, limit, offset, status, from, to);
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100 || !key.matches("[A-Za-z0-9._:-]+")) {
            throw new EnergyTransactionDomainException("Idempotency-Key is invalid");
        }
    }
}
