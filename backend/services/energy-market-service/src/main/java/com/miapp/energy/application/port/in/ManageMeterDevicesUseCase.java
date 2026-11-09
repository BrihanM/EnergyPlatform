package com.miapp.energy.application.port.in;

import com.miapp.energy.domain.model.MeterDevice;
import java.util.UUID;

/** Administrator-controlled lifecycle for technical meter identities. */
public interface ManageMeterDevicesUseCase {
    record Provisioned(MeterDevice device, String credential) {
        @Override public String toString() { return "Provisioned[credential=REDACTED]"; }
    }

    Provisioned provision(UUID actorId, UUID ownerId, String identifier);
    Provisioned rotate(UUID actorId, UUID id);
    MeterDevice setStatus(UUID actorId, UUID id, MeterDevice.Status status);
}
