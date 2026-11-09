package com.miapp.energy.infrastructure.adapter.out.postgresql;

import com.miapp.energy.application.port.out.MeterRepository;
import com.miapp.energy.domain.model.EnergyReading;
import com.miapp.energy.domain.model.MeterDevice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcMeterRepository implements MeterRepository {
    private final JdbcTemplate jdbc;

    public JdbcMeterRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean insert(MeterDevice device, byte[] hash) {
        try {
            return jdbc.update("""
                    INSERT INTO energy_market.meter_devices
                    (id, owner_user_id, device_identifier, credential_hash, status, registered_at)
                    VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (device_identifier) DO NOTHING
                    """, device.id(), device.ownerUserId(), device.deviceIdentifier(), hash,
                    device.status().name(), Timestamp.from(device.registeredAt())) == 1;
        } catch (DataIntegrityViolationException exception) {
            if (exception.getMostSpecificCause() instanceof java.sql.SQLException sql
                    && "23503".equals(sql.getSQLState())) {
                throw new IllegalArgumentException("Invalid device owner", exception);
            }
            throw exception;
        }
    }

    @Override
    public Optional<StoredDevice> lock(UUID id) {
        return jdbc.query("""
                SELECT id, owner_user_id, device_identifier, credential_hash, status, registered_at
                FROM energy_market.meter_devices WHERE id = ? FOR UPDATE
                """, (rs, row) -> new StoredDevice(new MeterDevice(
                rs.getObject("id", UUID.class), rs.getObject("owner_user_id", UUID.class),
                rs.getString("device_identifier"), MeterDevice.Status.valueOf(rs.getString("status")),
                rs.getTimestamp("registered_at").toInstant()), rs.getBytes("credential_hash")), id)
                .stream().findFirst();
    }

    @Override
    public void updateStatus(UUID id, MeterDevice.Status status) {
        jdbc.update("UPDATE energy_market.meter_devices SET status = ? WHERE id = ?", status.name(), id);
    }

    @Override
    public void updateCredential(UUID id, byte[] hash) {
        jdbc.update("UPDATE energy_market.meter_devices SET credential_hash = ? WHERE id = ?", hash, id);
    }

    @Override
    public boolean insertReading(EnergyReading reading) {
        return jdbc.update("""
                INSERT INTO energy_market.energy_readings
                (id, meter_device_id, generated_energy_kwh, surplus_energy_kwh, measured_at, received_at, validation_status)
                VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING
                """, reading.id(), reading.meterDeviceId(), reading.generatedEnergyKwh(), reading.surplusEnergyKwh(),
                Timestamp.from(reading.measuredAt()), Timestamp.from(reading.receivedAt()),
                reading.validationStatus().name()) == 1;
    }
}
