CREATE TABLE audit_events (
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
CREATE INDEX idx_market_audit_time ON audit_events (occurred_at DESC, id DESC);
CREATE INDEX idx_market_audit_actor_time ON audit_events (actor_id, occurred_at DESC);
CREATE INDEX idx_market_audit_type_time ON audit_events (event_type, occurred_at DESC);
