package com.miapp.energy.application.port.in;

import com.miapp.energy.domain.model.EnergyReading;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface SubmitMeterReadingUseCase {
    EnergyReading ingest(UUID deviceId, String credential, UUID readingId,
                         BigDecimal generated, BigDecimal surplus, Instant measured);
}
