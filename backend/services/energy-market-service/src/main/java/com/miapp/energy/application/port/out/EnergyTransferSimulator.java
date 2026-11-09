package com.miapp.energy.application.port.out;

import com.miapp.energy.domain.model.EnergyOffer;
import com.miapp.energy.domain.model.EnergyTransferResult;

public interface EnergyTransferSimulator {
    EnergyTransferResult transfer(EnergyOffer offer);
}
