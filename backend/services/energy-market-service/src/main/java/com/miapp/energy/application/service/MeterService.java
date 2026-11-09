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
    private final MarketAuditService audit;

    public MeterService(MeterRepository repository, MeterCredentials credentials, MarketAuditService audit) {
        this.repository = repository;
        this.credentials = credentials;
        this.audit = audit;
    }

    @Transactional
    public Provisioned provision(UUID actorId, UUID ownerId, String identifier) {
        MeterDevice device = new MeterDevice(UUID.randomUUID(), ownerId, identifier,
                MeterDevice.Status.ACTIVE, Instant.now());
        var issued = credentials.issue();
        if (!repository.insert(device, issued.hash())) throw new MeterException(MeterException.Kind.CONFLICT);
        audit.record(com.miapp.energy.domain.model.AuditEvent.Type.METER_PROVISIONED,
                com.miapp.energy.domain.model.AuditEvent.ActorType.USER, actorId, ownerId,
                com.miapp.energy.domain.model.AuditEvent.Resource.DEVICE, device.id(),
                com.miapp.energy.domain.model.AuditEvent.Result.SUCCESS);
        return new Provisioned(device, issued.value());
    }

    @Transactional
    public MeterDevice setStatus(UUID actorId, UUID id, MeterDevice.Status status) {
        var stored = repository.lock(id).orElseThrow(() -> new MeterException(MeterException.Kind.NOT_FOUND));
        if (stored.device().status() == MeterDevice.Status.REVOKED && status != MeterDevice.Status.REVOKED) {
            throw new MeterException(MeterException.Kind.CONFLICT);
        }
        MeterDevice updated = stored.device().changeStatus(status);
        repository.updateStatus(id, updated.status());
        if (stored.device().status() != updated.status()) {
            var type = switch (updated.status()) {
                case ACTIVE -> com.miapp.energy.domain.model.AuditEvent.Type.METER_ACTIVATED;
                case SUSPENDED -> com.miapp.energy.domain.model.AuditEvent.Type.METER_SUSPENDED;
                case REVOKED -> com.miapp.energy.domain.model.AuditEvent.Type.METER_REVOKED;
            };
            audit.record(type, com.miapp.energy.domain.model.AuditEvent.ActorType.USER, actorId,
                    updated.ownerUserId(), com.miapp.energy.domain.model.AuditEvent.Resource.DEVICE, id,
                    com.miapp.energy.domain.model.AuditEvent.Result.SUCCESS);
        }
        return updated;
    }

    @Transactional
    public Provisioned rotate(UUID actorId, UUID id) {
        var stored = repository.lock(id).orElseThrow(() -> new MeterException(MeterException.Kind.NOT_FOUND));
        if (stored.device().status() == MeterDevice.Status.REVOKED) {
            throw new MeterException(MeterException.Kind.CONFLICT);
        }
        var issued = credentials.issue();
        repository.updateCredential(id, issued.hash());
        audit.record(com.miapp.energy.domain.model.AuditEvent.Type.METER_CREDENTIAL_ROTATED,
                com.miapp.energy.domain.model.AuditEvent.ActorType.USER, actorId, stored.device().ownerUserId(),
                com.miapp.energy.domain.model.AuditEvent.Resource.DEVICE, id,
                com.miapp.energy.domain.model.AuditEvent.Result.SUCCESS);
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
        if (reading.validationStatus() == EnergyReading.Status.INCONSISTENT) {
            audit.record(com.miapp.energy.domain.model.AuditEvent.Type.READING_INCONSISTENT,
                    com.miapp.energy.domain.model.AuditEvent.ActorType.DEVICE, deviceId,
                    stored.get().device().ownerUserId(), com.miapp.energy.domain.model.AuditEvent.Resource.READING,
                    reading.id(), com.miapp.energy.domain.model.AuditEvent.Result.SUCCESS);
        }
        return reading;
    }
}
