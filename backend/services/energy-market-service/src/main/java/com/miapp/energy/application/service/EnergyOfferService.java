package com.miapp.energy.application.service;

import com.miapp.energy.application.port.in.*;
import com.miapp.energy.application.port.out.EnergyOfferRepository;
import com.miapp.energy.domain.exception.EnergyOfferDomainException;
import com.miapp.energy.domain.model.EnergyOffer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Coordinates offer use cases while keeping ownership and lifecycle rules in the domain.
 */
@Service
public class EnergyOfferService implements CreateEnergyOfferUseCase, GetAvailableOffersUseCase,
        GetEnergyOfferByIdUseCase, GetMyOffersUseCase, UpdateEnergyOfferUseCase,
        CancelEnergyOfferUseCase {

    private final EnergyOfferRepository repository;
    private final Clock clock;
    private final MarketAuditService audit;

    @Autowired
    public EnergyOfferService(EnergyOfferRepository repository, MarketAuditService audit) {
        this(repository, Clock.systemUTC(), audit);
    }

    EnergyOfferService(EnergyOfferRepository repository, Clock clock, MarketAuditService audit) {
        this.repository = repository;
        this.clock = clock;
        this.audit = audit;
    }

    @Override
    @Transactional
    public EnergyOffer create(UUID sellerId, BigDecimal energyAmountKwh, BigDecimal pricePerKwh,
                              Instant expiresAt) {
        EnergyOffer offer = repository.save(EnergyOffer.create(sellerId, energyAmountKwh, pricePerKwh,
                expiresAt, now()));
        audit.record(com.miapp.energy.domain.model.AuditEvent.Type.OFFER_CREATED,
                com.miapp.energy.domain.model.AuditEvent.ActorType.USER, sellerId, sellerId,
                com.miapp.energy.domain.model.AuditEvent.Resource.OFFER, offer.id(),
                com.miapp.energy.domain.model.AuditEvent.Result.SUCCESS);
        return offer;
    }

    @Override
    @Transactional
    public PageResult<EnergyOffer> getAvailable(OfferQuery query) {
        return repository.findAvailable(query);
    }

    @Override
    @Transactional
    public EnergyOffer getAvailableById(UUID offerId) {
        EnergyOffer offer = repository.findById(offerId)
                .orElseThrow(() -> new EnergyOfferDomainException("Offer was not found"));
        offer.expireIfNeeded(now());
        if (offer.status() != com.miapp.energy.domain.model.EnergyOfferStatus.ACTIVE) {
            if (offer.status() == com.miapp.energy.domain.model.EnergyOfferStatus.EXPIRED) {
                repository.markExpired(offer.id());
            }
            throw new EnergyOfferDomainException("Offer was not found");
        }
        return offer;
    }

    @Override
    @Transactional
    public PageResult<EnergyOffer> getMine(UUID sellerId, OfferQuery query) {
        return repository.findBySeller(sellerId, query);
    }

    @Override
    @Transactional
    public EnergyOffer update(UUID sellerId, UUID offerId, BigDecimal energyAmountKwh,
                              BigDecimal pricePerKwh, Instant expiresAt) {
        EnergyOffer offer = ownedOffer(sellerId, offerId);
        offer.update(energyAmountKwh, pricePerKwh, expiresAt, now());
        EnergyOffer updated = repository.update(offer);
        audit.record(com.miapp.energy.domain.model.AuditEvent.Type.OFFER_UPDATED,
                com.miapp.energy.domain.model.AuditEvent.ActorType.USER, sellerId, sellerId,
                com.miapp.energy.domain.model.AuditEvent.Resource.OFFER, offerId,
                com.miapp.energy.domain.model.AuditEvent.Result.SUCCESS);
        return updated;
    }

    @Override
    @Transactional
    public EnergyOffer cancel(UUID sellerId, UUID offerId) {
        EnergyOffer offer = ownedOffer(sellerId, offerId);
        offer.cancel(now());
        EnergyOffer cancelled = repository.update(offer);
        audit.record(com.miapp.energy.domain.model.AuditEvent.Type.OFFER_CANCELLED,
                com.miapp.energy.domain.model.AuditEvent.ActorType.USER, sellerId, sellerId,
                com.miapp.energy.domain.model.AuditEvent.Resource.OFFER, offerId,
                com.miapp.energy.domain.model.AuditEvent.Result.SUCCESS);
        return cancelled;
    }

    private EnergyOffer ownedOffer(UUID sellerId, UUID offerId) {
        EnergyOffer offer = repository.findById(offerId)
                .orElseThrow(() -> new EnergyOfferDomainException("Offer was not found"));
        if (!offer.sellerId().equals(sellerId)) {
            throw new EnergyOfferDomainException("Offer was not found");
        }
        return offer;
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
