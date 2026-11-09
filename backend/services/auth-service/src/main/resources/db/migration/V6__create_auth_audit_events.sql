CREATE TABLE auth_audit_events (
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
CREATE INDEX idx_auth_audit_time ON auth_audit_events (occurred_at DESC, id DESC);
CREATE INDEX idx_auth_audit_actor_time ON auth_audit_events (actor_id, occurred_at DESC);
CREATE INDEX idx_auth_audit_type_time ON auth_audit_events (event_type, occurred_at DESC);
