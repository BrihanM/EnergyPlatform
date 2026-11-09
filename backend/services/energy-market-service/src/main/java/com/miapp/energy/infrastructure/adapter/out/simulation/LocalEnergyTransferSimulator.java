package com.miapp.energy.infrastructure.adapter.out.simulation;

import com.miapp.energy.application.port.out.EnergyTransferSimulator;
import com.miapp.energy.domain.model.EnergyOffer;
import com.miapp.energy.domain.model.EnergyTransferResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class LocalEnergyTransferSimulator implements EnergyTransferSimulator {
    private final String mode;
    private final Clock clock;

    public LocalEnergyTransferSimulator(
            @Value("${energy-market.simulation.mode:SUCCESS}") String mode) {
        this.mode = mode.trim().toUpperCase();
        this.clock = Clock.systemUTC();
        if (!this.mode.equals("SUCCESS") && !this.mode.equals("REJECT")
                && !this.mode.equals("RULE_BASED")) {
            throw new IllegalArgumentException("Unsupported energy-market.simulation.mode");
        }
    }

    @Override
    public EnergyTransferResult transfer(EnergyOffer offer) {
        Instant now = Instant.now(clock);
        boolean success = switch (mode) {
            case "SUCCESS" -> true;
            case "REJECT" -> false;
            case "RULE_BASED" -> offer.energyAmountKwh().signum() > 0
                    && offer.pricePerKwh().signum() > 0;
            default -> throw new IllegalStateException("Unsupported simulation mode");
        };
        return new EnergyTransferResult(success, offer.energyAmountKwh(), UUID.randomUUID(), now,
                success ? "Simulated transfer accepted" : "Simulated transfer rejected");
    }
}
