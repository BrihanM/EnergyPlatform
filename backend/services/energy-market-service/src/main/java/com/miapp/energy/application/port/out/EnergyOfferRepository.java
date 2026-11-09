package com.miapp.energy.application.port.out;

import com.miapp.energy.application.service.OfferQuery;
import com.miapp.energy.application.service.PageResult;
import com.miapp.energy.domain.model.EnergyOffer;
import com.miapp.energy.domain.model.EnergyOfferStatus;

import java.util.Optional;
import java.util.UUID;

public interface EnergyOfferRepository {
    EnergyOffer save(EnergyOffer offer);
    Optional<EnergyOffer> findById(UUID offerId);
    PageResult<EnergyOffer> findAvailable(OfferQuery query);
    PageResult<EnergyOffer> findBySeller(UUID sellerId, OfferQuery query);
    EnergyOffer update(EnergyOffer offer);
    int markExpired(UUID offerId);
    EnergyOffer findForPurchase(UUID offerId);
    EnergyOffer setPurchaseStatus(UUID offerId, EnergyOfferStatus status);
}
