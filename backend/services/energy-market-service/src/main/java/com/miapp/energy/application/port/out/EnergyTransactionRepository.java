package com.miapp.energy.application.port.out;

import com.miapp.energy.domain.model.EnergyTransaction;
import com.miapp.energy.domain.model.VerificationResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnergyTransactionRepository {
    Optional<EnergyTransaction> findByBuyerAndKey(UUID buyerId, String key);
    boolean claimIdempotency(UUID buyerId, String key, UUID offerId);
    EnergyTransaction save(EnergyTransaction transaction);
    EnergyTransaction update(EnergyTransaction transaction);
    void saveVerification(UUID transactionId, VerificationResult result, String reason,
                          UUID simulationReference, Instant verifiedAt);
    Optional<EnergyTransaction> findById(UUID id);
    Optional<VerificationSnapshot> findVerification(UUID transactionId);
    List<EnergyTransaction> findForUser(UUID userId, int limit, int offset,
                                         EnergyTransactionStatusFilter status,
                                         Instant from, Instant to);

    enum EnergyTransactionStatusFilter { PENDING_VERIFICATION, CONFIRMED, REJECTED, CANCELLED }

    record VerificationSnapshot(VerificationResult result, String reason,
                                UUID simulationReference, Instant verifiedAt) {}
}
