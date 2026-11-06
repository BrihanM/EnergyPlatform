package com.miapp.energy.application.port.in;

import com.miapp.energy.application.service.OfferQuery;
import com.miapp.energy.application.service.PageResult;
import com.miapp.energy.domain.model.EnergyOffer;

import java.util.UUID;

public interface GetMyOffersUseCase {
    PageResult<EnergyOffer> getMine(UUID sellerId, OfferQuery query);
}
