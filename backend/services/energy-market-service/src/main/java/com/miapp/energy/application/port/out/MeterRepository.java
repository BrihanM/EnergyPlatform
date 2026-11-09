package com.miapp.energy.application.port.out;

import com.miapp.energy.domain.model.EnergyReading;
import com.miapp.energy.domain.model.MeterDevice;
import java.util.Optional;
import java.util.UUID;

public interface MeterRepository {
    record StoredDevice(MeterDevice device, byte[] credentialHash) {}

    boolean insert(MeterDevice device, byte[] credentialHash);
    Optional<StoredDevice> lock(UUID id);
    void updateStatus(UUID id, MeterDevice.Status status);
    void updateCredential(UUID id, byte[] credentialHash);
    boolean insertReading(EnergyReading reading);
}
