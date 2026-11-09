CREATE TABLE rotated_refresh_token_history (
    refresh_token_hash VARCHAR(64) PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES sessions (id) ON DELETE CASCADE,
    rotated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_rotated_refresh_hash_length CHECK (char_length(refresh_token_hash) = 43)
);
CREATE INDEX idx_rotated_refresh_session ON rotated_refresh_token_history (session_id);
CREATE INDEX idx_sessions_expiration ON sessions (expires_at);
