package com.miapp.energy.application.service;

import com.miapp.energy.application.port.in.ManageMeterDevicesUseCase;
import com.miapp.energy.application.port.in.SubmitMeterReadingUseCase;
import com.miapp.energy.application.port.out.MeterCredentials;
import com.miapp.energy.application.port.out.MeterRepository;
import com.miapp.energy.domain.model.EnergyReading;
import com.miapp.energy.domain.model.MeterDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Coordinates lifecycle and reading ingestion under database row locks. */
@Service
public class MeterService implements ManageMeterDevicesUseCase, SubmitMeterReadingUseCase {
    private static final Logger LOG = LoggerFactory.getLogger(MeterService.class);
    private static final byte[] DUMMY_HASH = new byte[32];
    private final MeterRepository repository;
    private final MeterCredentials credentials;

    public MeterService(MeterRepository repository, MeterCredentials credentials) {
        this.repository = repository;
        this.credentials = credentials;
    }

    @Transactional
    public Provisioned provision(UUID ownerId, String identifier) {
        MeterDevice device = new MeterDevice(UUID.randomUUID(), ownerId, identifier,
                MeterDevice.Status.ACTIVE, Instant.now());
        var issued = credentials.issue();
        if (!repository.insert(device, issued.hash())) throw new MeterException(MeterException.Kind.CONFLICT);
        return new Provisioned(device, issued.value());
    }

    @Transactional
    public MeterDevice setStatus(UUID id, MeterDevice.Status status) {
        var stored = repository.lock(id).orElseThrow(() -> new MeterException(MeterException.Kind.NOT_FOUND));
        if (stored.device().status() == MeterDevice.Status.REVOKED && status != MeterDevice.Status.REVOKED) {
            throw new MeterException(MeterException.Kind.CONFLICT);
        }
        MeterDevice updated = stored.device().changeStatus(status);
        repository.updateStatus(id, updated.status());
        return updated;
    }

    @Transactional
    public Provisioned rotate(UUID id) {
        var stored = repository.lock(id).orElseThrow(() -> new MeterException(MeterException.Kind.NOT_FOUND));
        if (stored.device().status() == MeterDevice.Status.REVOKED) {
            throw new MeterException(MeterException.Kind.CONFLICT);
        }
        var issued = credentials.issue();
        repository.updateCredential(id, issued.hash());
        return new Provisioned(stored.device(), issued.value());
    }

    @Transactional
    public EnergyReading ingest(UUID deviceId, String credential, UUID readingId,
                                BigDecimal generated, BigDecimal surplus, Instant measured) {
        var stored = repository.lock(deviceId);
        boolean matches = credentials.matches(credential, stored.map(MeterRepository.StoredDevice::credentialHash)
                .orElse(DUMMY_HASH));
        if (!matches || stored.isEmpty() || stored.get().device().status() != MeterDevice.Status.ACTIVE) {
            LOG.atWarn().addKeyValue("event", "meter_authentication_failed").log("Meter authentication failed");
            throw new MeterException(MeterException.Kind.UNAUTHORIZED);
        }
        EnergyReading reading = EnergyReading.classify(readingId, deviceId, generated, surplus, measured, Instant.now());
        if (!repository.insertReading(reading)) throw new MeterException(MeterException.Kind.CONFLICT);
        return reading;
    }
}
