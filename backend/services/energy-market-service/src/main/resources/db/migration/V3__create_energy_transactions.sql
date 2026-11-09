CREATE TABLE energy_market.energy_transactions (
    id UUID NOT NULL,
    offer_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    buyer_id UUID NOT NULL,
    energy_amount_kwh NUMERIC(12,3) NOT NULL,
    price_per_kwh NUMERIC(14,2) NOT NULL,
    total_amount NUMERIC(16,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    status VARCHAR(24) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT pk_energy_transactions PRIMARY KEY (id),
    CONSTRAINT fk_energy_transactions_offer FOREIGN KEY (offer_id)
        REFERENCES energy_market.energy_offers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_energy_transactions_seller FOREIGN KEY (seller_id)
        REFERENCES public.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_energy_transactions_buyer FOREIGN KEY (buyer_id)
        REFERENCES public.users (id) ON DELETE RESTRICT,
    CONSTRAINT uq_energy_transactions_buyer_key UNIQUE (buyer_id, idempotency_key),
    CONSTRAINT ck_energy_transactions_different_users CHECK (seller_id <> buyer_id),
    CONSTRAINT ck_energy_transactions_energy_positive CHECK (energy_amount_kwh > 0),
    CONSTRAINT ck_energy_transactions_price_positive CHECK (price_per_kwh > 0),
    CONSTRAINT ck_energy_transactions_total_positive CHECK (total_amount > 0),
    CONSTRAINT ck_energy_transactions_currency CHECK (currency = 'COP'),
    CONSTRAINT ck_energy_transactions_status CHECK (
        status IN ('PENDING_VERIFICATION', 'CONFIRMED', 'REJECTED', 'CANCELLED')
    )
);

CREATE TABLE energy_market.operator_verifications (
    id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    result VARCHAR(16) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    simulation_reference UUID NOT NULL,
    verified_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_operator_verifications PRIMARY KEY (id),
    CONSTRAINT uq_operator_verifications_transaction UNIQUE (transaction_id),
    CONSTRAINT fk_operator_verifications_transaction FOREIGN KEY (transaction_id)
        REFERENCES energy_market.energy_transactions (id) ON DELETE RESTRICT,
    CONSTRAINT ck_operator_verifications_result CHECK (result IN ('ACCEPTED', 'REJECTED'))
);

CREATE INDEX idx_energy_transactions_buyer_created
    ON energy_market.energy_transactions (buyer_id, created_at DESC);
CREATE INDEX idx_energy_transactions_seller_created
    ON energy_market.energy_transactions (seller_id, created_at DESC);
CREATE INDEX idx_energy_transactions_offer
    ON energy_market.energy_transactions (offer_id);
CREATE INDEX idx_energy_transactions_status_created
    ON energy_market.energy_transactions (status, created_at DESC);
CREATE INDEX idx_operator_verifications_verified_at
    ON energy_market.operator_verifications (verified_at);

CREATE TABLE energy_market.transaction_idempotency_keys (
    buyer_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    offer_id UUID NOT NULL,
    transaction_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_transaction_idempotency_keys PRIMARY KEY (buyer_id, idempotency_key),
    CONSTRAINT fk_transaction_idempotency_buyer FOREIGN KEY (buyer_id)
        REFERENCES public.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transaction_idempotency_offer FOREIGN KEY (offer_id)
        REFERENCES energy_market.energy_offers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transaction_idempotency_transaction FOREIGN KEY (transaction_id)
        REFERENCES energy_market.energy_transactions (id) ON DELETE RESTRICT
);
