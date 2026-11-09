DO $$
DECLARE
    runtime_role text := '${marketRuntimeRole}';
    auth_runtime text := '${authRuntimeRole}';
BEGIN
    IF runtime_role !~ '^[a-z_][a-z0-9_]{0,62}$' OR auth_runtime !~ '^[a-z_][a-z0-9_]{0,62}$'
       OR runtime_role = current_user OR auth_runtime = current_user OR auth_runtime = runtime_role THEN
        RAISE EXCEPTION 'Invalid or non-separated database roles';
    END IF;
    EXECUTE format('ALTER DEFAULT PRIVILEGES REVOKE ALL ON TABLES FROM PUBLIC, %I, %I', runtime_role, auth_runtime);
    EXECUTE format('ALTER DEFAULT PRIVILEGES REVOKE ALL ON SEQUENCES FROM PUBLIC, %I, %I', runtime_role, auth_runtime);
    EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA energy_market REVOKE ALL ON TABLES FROM PUBLIC, %I, %I', runtime_role, auth_runtime);
    EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA energy_market REVOKE ALL ON SEQUENCES FROM PUBLIC, %I, %I', runtime_role, auth_runtime);
    EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA energy_market FROM PUBLIC, %I, %I', runtime_role, auth_runtime);
    EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA energy_market FROM PUBLIC, %I, %I', runtime_role, auth_runtime);
    EXECUTE format('GRANT SELECT, INSERT, UPDATE ON energy_market.energy_offers, energy_market.energy_transactions, energy_market.transaction_idempotency_keys, energy_market.meter_devices TO %I', runtime_role);
    EXECUTE format('GRANT SELECT, INSERT ON energy_market.operator_verifications, energy_market.energy_readings TO %I', runtime_role);
    EXECUTE format('GRANT SELECT, INSERT ON energy_market.audit_events TO %I', runtime_role);
END $$;
