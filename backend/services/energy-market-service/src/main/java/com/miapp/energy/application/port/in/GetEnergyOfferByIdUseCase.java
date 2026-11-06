package com.miapp.energy.application.port.in;

import com.miapp.energy.domain.model.EnergyOffer;

import java.util.UUID;

public interface GetEnergyOfferByIdUseCase {
    EnergyOffer getAvailableById(UUID offerId);
}
