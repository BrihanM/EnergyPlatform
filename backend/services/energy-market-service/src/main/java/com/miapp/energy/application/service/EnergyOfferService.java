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

    @Autowired
    public EnergyOfferService(EnergyOfferRepository repository) {
        this(repository, Clock.systemUTC());
    }

    EnergyOfferService(EnergyOfferRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EnergyOffer create(UUID sellerId, BigDecimal energyAmountKwh, BigDecimal pricePerKwh,
                              Instant expiresAt) {
        return repository.save(EnergyOffer.create(sellerId, energyAmountKwh, pricePerKwh,
                expiresAt, now()));
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
        return repository.update(offer);
    }

    @Override
    @Transactional
    public EnergyOffer cancel(UUID sellerId, UUID offerId) {
        EnergyOffer offer = ownedOffer(sellerId, offerId);
        offer.cancel(now());
        return repository.update(offer);
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
