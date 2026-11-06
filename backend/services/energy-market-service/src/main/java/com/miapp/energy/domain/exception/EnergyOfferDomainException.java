package com.miapp.energy.domain.exception;

/**
 * Raised when an energy offer violates a business invariant.
 */
public class EnergyOfferDomainException extends RuntimeException {

    public EnergyOfferDomainException(String message) {
        super(message);
    }
}
