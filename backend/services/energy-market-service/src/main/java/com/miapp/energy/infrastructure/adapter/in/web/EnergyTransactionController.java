package com.miapp.energy.infrastructure.adapter.in.web;

import com.miapp.energy.application.port.out.EnergyTransactionRepository;
import com.miapp.energy.application.service.EnergyTransactionService;
import com.miapp.energy.domain.model.EnergyTransaction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class EnergyTransactionController {
    private final EnergyTransactionService service;

    public EnergyTransactionController(EnergyTransactionService service) {
        this.service = service;
    }

    @PostMapping
    public EnergyTransactionResponse purchase(@Valid @RequestBody PurchaseRequest request,
                                               @RequestHeader(value = "Idempotency-Key", required = false) String key,
                                               HttpServletRequest httpRequest) {
        EnergyTransaction transaction = service.purchase(
                OfferIdentity.requireNormalUser(httpRequest), request.offerId(), key);
        return EnergyTransactionResponse.from(transaction, service.verification(transaction.id()).orElse(null));
    }

    @GetMapping("/me")
    public List<EnergyTransactionResponse> mine(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) EnergyTransactionRepository.EnergyTransactionStatusFilter status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            HttpServletRequest request) {
        UUID userId = OfferIdentity.requireNormalUser(request);
        return service.mine(userId, size, offset, status, from, to).stream()
                .map(transaction -> EnergyTransactionResponse.from(transaction,
                        service.verification(transaction.id()).orElse(null))).toList();
    }

    @GetMapping("/{id}")
    public EnergyTransactionResponse detail(@PathVariable UUID id, HttpServletRequest request) {
        EnergyTransaction transaction = service.get(OfferIdentity.requireNormalUser(request), id);
        return EnergyTransactionResponse.from(transaction, service.verification(transaction.id()).orElse(null));
    }

    public record PurchaseRequest(@NotNull UUID offerId) {}

    public record EnergyTransactionResponse(UUID id, UUID offerId, UUID sellerId, UUID buyerId,
                                            java.math.BigDecimal energyAmountKwh,
                                            java.math.BigDecimal pricePerKwh,
                                            java.math.BigDecimal totalAmount, String currency,
                                            String status, Instant createdAt, Instant completedAt,
                                            String verificationResult, String verificationReason,
                                            UUID simulationReference, Instant verifiedAt) {
        static EnergyTransactionResponse from(EnergyTransaction transaction,
                                              EnergyTransactionRepository.VerificationSnapshot verification) {
            return new EnergyTransactionResponse(transaction.id(), transaction.offerId(),
                    transaction.sellerId(), transaction.buyerId(), transaction.energyAmountKwh(),
                    transaction.pricePerKwh(), transaction.totalAmount(), transaction.currency(),
                    transaction.status().name(), transaction.createdAt(), transaction.completedAt(),
                    verification == null ? null : verification.result().name(),
                    verification == null ? null : verification.reason(),
                    verification == null ? null : verification.simulationReference(),
                    verification == null ? null : verification.verifiedAt());
        }
    }
}
