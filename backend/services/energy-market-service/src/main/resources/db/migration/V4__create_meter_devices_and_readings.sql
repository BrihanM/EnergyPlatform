CREATE TABLE energy_market.meter_devices (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE RESTRICT,
    device_identifier VARCHAR(128) NOT NULL UNIQUE,
    credential_hash BYTEA NOT NULL CHECK (octet_length(credential_hash) = 32),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    registered_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_meter_devices_owner ON energy_market.meter_devices(owner_user_id);

CREATE TABLE energy_market.energy_readings (
    id UUID PRIMARY KEY,
    meter_device_id UUID NOT NULL REFERENCES energy_market.meter_devices(id) ON DELETE RESTRICT,
    generated_energy_kwh NUMERIC(18,6) NOT NULL CHECK (generated_energy_kwh >= 0),
    surplus_energy_kwh NUMERIC(18,6) NOT NULL CHECK (surplus_energy_kwh >= 0),
    measured_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    validation_status VARCHAR(16) NOT NULL CHECK (
        validation_status IN ('PENDING', 'VALID', 'INCONSISTENT', 'REJECTED')
    ),
    CONSTRAINT ck_reading_measured_before_receipt CHECK (measured_at <= received_at),
    CONSTRAINT ck_valid_reading_surplus CHECK (
        validation_status <> 'VALID' OR surplus_energy_kwh <= generated_energy_kwh
    )
);

CREATE INDEX idx_energy_readings_device_measured
    ON energy_market.energy_readings(meter_device_id, measured_at DESC);
