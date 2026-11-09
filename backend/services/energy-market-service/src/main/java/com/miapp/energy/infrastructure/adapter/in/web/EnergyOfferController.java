package com.miapp.energy.infrastructure.adapter.in.web;

import com.miapp.energy.application.port.in.*;
import com.miapp.energy.application.service.OfferQuery;
import com.miapp.energy.domain.model.EnergyOfferStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * REST adapter for publication and consultation of energy offers.
 */
@RestController
@RequestMapping({"/api/v1/offers", "/api/v1/market/offers"})
public class EnergyOfferController {

    private final CreateEnergyOfferUseCase create;
    private final GetAvailableOffersUseCase available;
    private final GetEnergyOfferByIdUseCase byId;
    private final GetMyOffersUseCase mine;
    private final UpdateEnergyOfferUseCase update;
    private final CancelEnergyOfferUseCase cancel;

    public EnergyOfferController(CreateEnergyOfferUseCase create, GetAvailableOffersUseCase available,
                                 GetEnergyOfferByIdUseCase byId, GetMyOffersUseCase mine,
                                 UpdateEnergyOfferUseCase update, CancelEnergyOfferUseCase cancel) {
        this.create = create;
        this.available = available;
        this.byId = byId;
        this.mine = mine;
        this.update = update;
        this.cancel = cancel;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnergyOfferResponse create(@Valid @RequestBody EnergyOfferRequest request,
                                      HttpServletRequest httpRequest) {
        UUID sellerId = OfferIdentity.requireNormalUser(httpRequest);
        return EnergyOfferResponse.from(create.create(sellerId, request.energyAmountKwh(),
                request.pricePerKwh(), request.expiresAt()));
    }

    @GetMapping
    public OfferPageResponse available(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(defaultValue = "createdAt") String sort,
                                       @RequestParam(required = false) BigDecimal minEnergy,
                                       @RequestParam(required = false) BigDecimal maxEnergy,
                                       @RequestParam(required = false) BigDecimal minPrice,
                                       @RequestParam(required = false) BigDecimal maxPrice,
                                       @RequestParam(required = false) Instant expiresBefore,
                                       @RequestParam(required = false) UUID id) {
        return OfferPageResponse.from(available.getAvailable(OfferQuery.available(page, size, sort,
                minEnergy, maxEnergy, minPrice, maxPrice, expiresBefore, id)));
    }

    @GetMapping("/{offerId}")
    public EnergyOfferResponse byId(@PathVariable UUID offerId) {
        return EnergyOfferResponse.from(byId.getAvailableById(offerId));
    }

    @GetMapping("/me")
    public OfferPageResponse mine(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(defaultValue = "createdAt") String sort,
                                  @RequestParam(required = false) EnergyOfferStatus status,
                                  HttpServletRequest request) {
        UUID sellerId = OfferIdentity.requireNormalUser(request);
        OfferQuery query = new OfferQuery(null, null, null, null, null, null, status,
                page, size, sort);
        return OfferPageResponse.from(mine.getMine(sellerId, query));
    }

    @PutMapping("/{offerId}")
    public EnergyOfferResponse update(@PathVariable UUID offerId,
                                      @Valid @RequestBody EnergyOfferRequest request,
                                      HttpServletRequest httpRequest) {
        UUID sellerId = OfferIdentity.requireNormalUser(httpRequest);
        return EnergyOfferResponse.from(update.update(sellerId, offerId, request.energyAmountKwh(),
                request.pricePerKwh(), request.expiresAt()));
    }

    @DeleteMapping("/{offerId}")
    public EnergyOfferResponse cancel(@PathVariable UUID offerId,
                                      HttpServletRequest httpRequest) {
        UUID sellerId = OfferIdentity.requireNormalUser(httpRequest);
        return EnergyOfferResponse.from(cancel.cancel(sellerId, offerId));
    }
}
