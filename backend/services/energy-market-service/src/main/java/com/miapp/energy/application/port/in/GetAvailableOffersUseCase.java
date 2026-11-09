package com.miapp.energy.application.port.in;

import com.miapp.energy.application.service.OfferQuery;
import com.miapp.energy.application.service.PageResult;
import com.miapp.energy.domain.model.EnergyOffer;

public interface GetAvailableOffersUseCase {
    PageResult<EnergyOffer> getAvailable(OfferQuery query);
}
