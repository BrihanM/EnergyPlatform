package com.miapp.energy.infrastructure.adapter.in.web;

import com.miapp.energy.application.service.PageResult;
import com.miapp.energy.domain.model.EnergyOffer;

import java.util.List;

public record OfferPageResponse(List<EnergyOfferResponse> content, int page, int size,
                                long totalElements, int totalPages) {
    public static OfferPageResponse from(PageResult<EnergyOffer> result) {
        return new OfferPageResponse(result.content().stream().map(EnergyOfferResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
