CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_sessions_account
        FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT ck_sessions_expiration
        CHECK (expires_at >= created_at),
    CONSTRAINT ck_sessions_hash_length
        CHECK (char_length(refresh_token_hash) = 43)
);

CREATE INDEX idx_sessions_account_id ON sessions (account_id);
CREATE INDEX idx_sessions_refresh_token_hash ON sessions (refresh_token_hash);
