CREATE TABLE energy_market.energy_offers (
    id UUID NOT NULL,
    seller_id UUID NOT NULL,
    energy_amount_kwh NUMERIC(12,3) NOT NULL,
    price_per_kwh NUMERIC(14,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_energy_offers PRIMARY KEY (id),
    CONSTRAINT fk_energy_offers_seller FOREIGN KEY (seller_id)
        REFERENCES public.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_energy_offers_energy_positive CHECK (energy_amount_kwh > 0),
    CONSTRAINT ck_energy_offers_price_positive CHECK (price_per_kwh > 0),
    CONSTRAINT ck_energy_offers_status CHECK (
        status IN ('ACTIVE', 'RESERVED', 'SOLD', 'CANCELLED', 'EXPIRED')
    ),
    CONSTRAINT ck_energy_offers_dates CHECK (expires_at > created_at)
);

CREATE INDEX idx_energy_offers_active_expiry
    ON energy_market.energy_offers (status, expires_at, created_at);

CREATE INDEX idx_energy_offers_seller_updated
    ON energy_market.energy_offers (seller_id, updated_at);

CREATE INDEX idx_energy_offers_price
    ON energy_market.energy_offers (price_per_kwh, created_at);
