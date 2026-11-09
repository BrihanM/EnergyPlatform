-- Esquema completo consolidado de EnergyPlatform (equivalente a Auth V1-V8 y Energy Market V1-V5).
-- Uso: solo para bases de datos NUEVAS y vacías. Para entornos existentes, usar Flyway.
-- No incluye roles ni privilegios: ver infrastructure/postgres/bootstrap-roles.sql y los R__*_privileges.sql.
-- Ejecutar como superusuario o como el propietario de la base, dentro de una sola transacción.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS energy_market;

-- =====================================================================
-- AUTH SERVICE (esquema public)
-- =====================================================================

-- Auth V1, V2 (nombres cifrados en V8). Los nombres se guardan cifrados con pgp_sym_encrypt.
CREATE TABLE public.users (
    id UUID NOT NULL,
    first_name_enc BYTEA NOT NULL,
    last_name_enc BYTEA NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

-- Auth V1, V2. login_identifier se sustituyó por username y email normalizados en minúsculas.
CREATE TABLE public.accounts (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id)
        REFERENCES public.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT ck_accounts_username_normalized CHECK (username = LOWER(username)),
    CONSTRAINT ck_accounts_email_normalized CHECK (email = LOWER(email))
);

CREATE INDEX idx_accounts_user_id ON public.accounts (user_id);
CREATE UNIQUE INDEX uk_accounts_username ON public.accounts (username);
CREATE UNIQUE INDEX uk_accounts_email ON public.accounts (email);

-- Auth V3, V4 (hash SHA-256 en Base64 = 43 caracteres).
CREATE TABLE public.sessions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    refresh_token_hash VARCHAR(43) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_sessions_account
        FOREIGN KEY (account_id) REFERENCES public.accounts (id),
    CONSTRAINT ck_sessions_expiration
        CHECK (expires_at >= created_at),
    CONSTRAINT ck_sessions_hash_length
        CHECK (char_length(refresh_token_hash) = 43)
);

CREATE INDEX idx_sessions_account_id ON public.sessions (account_id);
CREATE INDEX idx_sessions_refresh_token_hash ON public.sessions (refresh_token_hash);
CREATE INDEX idx_sessions_expiration ON public.sessions (expires_at);

-- Auth V5 (roles RBAC).
CREATE TABLE public.roles (
    code VARCHAR(32) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (code),
    CONSTRAINT ck_roles_code CHECK (code IN ('NORMAL_USER', 'ADMIN', 'SECURITY_AUDITOR'))
);

CREATE TABLE public.user_roles (
    user_id UUID NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES public.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_code)
        REFERENCES public.roles (code) ON DELETE RESTRICT
);

CREATE INDEX idx_user_roles_role_code ON public.user_roles (role_code);

INSERT INTO public.roles (code) VALUES
    ('NORMAL_USER'),
    ('ADMIN'),
    ('SECURITY_AUDITOR');

-- Auth V6.
CREATE TABLE public.auth_audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(48) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_type VARCHAR(16) NOT NULL CHECK (actor_type IN ('USER', 'ANONYMOUS', 'SYSTEM')),
    actor_id UUID,
    affected_user_id UUID,
    resource_type VARCHAR(24) NOT NULL,
    resource_id UUID,
    role_code VARCHAR(32) CHECK (role_code IN ('NORMAL_USER', 'ADMIN', 'SECURITY_AUDITOR')),
    result VARCHAR(16) NOT NULL
);

CREATE INDEX idx_auth_audit_time ON public.auth_audit_events (occurred_at DESC, id DESC);
CREATE INDEX idx_auth_audit_actor_time ON public.auth_audit_events (actor_id, occurred_at DESC);
CREATE INDEX idx_auth_audit_type_time ON public.auth_audit_events (event_type, occurred_at DESC);

-- Auth V7 (historial de refresh tokens rotados para detección de reutilización).
CREATE TABLE public.rotated_refresh_token_history (
    refresh_token_hash VARCHAR(64) PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES public.sessions (id) ON DELETE CASCADE,
    rotated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_rotated_refresh_hash_length CHECK (char_length(refresh_token_hash) = 43)
);

CREATE INDEX idx_rotated_refresh_session ON public.rotated_refresh_token_history (session_id);

-- =====================================================================
-- ENERGY MARKET SERVICE (esquema energy_market)
-- =====================================================================

-- Market V2.
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

-- Market V3.
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

-- Market V4. credential_hash guarda 32 bytes (hash de la credencial del dispositivo, nunca en claro).
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

-- Market V5.
CREATE TABLE energy_market.audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(48) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_type VARCHAR(16) NOT NULL CHECK (actor_type IN ('USER', 'DEVICE')),
    actor_id UUID NOT NULL,
    affected_user_id UUID,
    resource_type VARCHAR(24) NOT NULL,
    resource_id UUID NOT NULL,
    result VARCHAR(16) NOT NULL
);

CREATE INDEX idx_market_audit_time ON energy_market.audit_events (occurred_at DESC, id DESC);
CREATE INDEX idx_market_audit_actor_time ON energy_market.audit_events (actor_id, occurred_at DESC);
CREATE INDEX idx_market_audit_type_time ON energy_market.audit_events (event_type, occurred_at DESC);

-- =====================================================================
-- HISTORIAL DE VERSIONES (formato de Flyway)
-- Registro informativo de las migraciones consolidadas. checksum NULL: Flyway no lo recalcula aquí.
-- Si Flyway valida esta base y reporta diferencias de checksum, usar "flyway repair" con la versión de la migración real.
-- =====================================================================

CREATE TABLE public.flyway_schema_history (
    installed_rank INTEGER NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INTEGER,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT now(),
    execution_time INTEGER NOT NULL,
    success BOOLEAN NOT NULL,
    CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
);

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history (success);

INSERT INTO public.flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
VALUES
    (1, '1', 'create auth schema', 'SQL', 'V1__create_auth_schema.sql', NULL, CURRENT_USER, 0, TRUE),
    (2, '2', 'register user account', 'SQL', 'V2__register_user_account.sql', NULL, CURRENT_USER, 0, TRUE),
    (3, '3', 'create sessions', 'SQL', 'V3__create_sessions.sql', NULL, CURRENT_USER, 0, TRUE),
    (4, '4', 'align refresh token hash length', 'SQL', 'V4__align_refresh_token_hash_length.sql', NULL, CURRENT_USER, 0, TRUE),
    (5, '5', 'create roles', 'SQL', 'V5__create_roles.sql', NULL, CURRENT_USER, 0, TRUE),
    (6, '6', 'create auth audit events', 'SQL', 'V6__create_auth_audit_events.sql', NULL, CURRENT_USER, 0, TRUE),
    (7, '7', 'create rotated refresh token history', 'SQL', 'V7__create_rotated_refresh_token_history.sql', NULL, CURRENT_USER, 0, TRUE),
    (8, '8', 'encrypt user names with pgcrypto', 'SQL', 'V8__encrypt_user_names_with_pgcrypto.sql', NULL, CURRENT_USER, 0, TRUE);

CREATE TABLE energy_market.flyway_schema_history (
    installed_rank INTEGER NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INTEGER,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT now(),
    execution_time INTEGER NOT NULL,
    success BOOLEAN NOT NULL,
    CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
);

CREATE INDEX flyway_schema_history_s_idx ON energy_market.flyway_schema_history (success);

INSERT INTO energy_market.flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
VALUES
    (1, '1', 'initialize energy market schema', 'SQL', 'V1__initialize_energy_market_schema.sql', NULL, CURRENT_USER, 0, TRUE),
    (2, '2', 'create energy offers', 'SQL', 'V2__create_energy_offers.sql', NULL, CURRENT_USER, 0, TRUE),
    (3, '3', 'create energy transactions', 'SQL', 'V3__create_energy_transactions.sql', NULL, CURRENT_USER, 0, TRUE),
    (4, '4', 'create meter devices and readings', 'SQL', 'V4__create_meter_devices_and_readings.sql', NULL, CURRENT_USER, 0, TRUE),
    (5, '5', 'create market audit events', 'SQL', 'V5__create_market_audit_events.sql', NULL, CURRENT_USER, 0, TRUE);

COMMIT;
