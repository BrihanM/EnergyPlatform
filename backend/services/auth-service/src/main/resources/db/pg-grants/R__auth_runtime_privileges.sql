DO $$
DECLARE
    runtime_role text := '${authRuntimeRole}';
    market_owner text := '${marketMigrationRole}';
    market_runtime text := '${marketRuntimeRole}';
BEGIN
    IF runtime_role !~ '^[a-z_][a-z0-9_]{0,62}$' OR market_owner !~ '^[a-z_][a-z0-9_]{0,62}$'
       OR market_runtime !~ '^[a-z_][a-z0-9_]{0,62}$' OR runtime_role = current_user
       OR market_owner = current_user OR market_runtime = current_user
       OR runtime_role = market_owner OR runtime_role = market_runtime OR market_owner = market_runtime THEN
        RAISE EXCEPTION 'Invalid or non-separated database roles';
    END IF;
    EXECUTE format('ALTER DEFAULT PRIVILEGES REVOKE ALL ON TABLES FROM PUBLIC, %I, %I, %I', runtime_role, market_runtime, market_owner);
    EXECUTE format('ALTER DEFAULT PRIVILEGES REVOKE ALL ON SEQUENCES FROM PUBLIC, %I, %I, %I', runtime_role, market_runtime, market_owner);
    EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM PUBLIC, %I, %I, %I', runtime_role, market_runtime, market_owner);
    EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM PUBLIC, %I, %I, %I', runtime_role, market_runtime, market_owner);
    EXECUTE format('REVOKE ALL ON public.users, public.accounts, public.sessions, public.roles, public.user_roles, public.rotated_refresh_token_history, public.auth_audit_events, public.flyway_schema_history FROM PUBLIC, %I, %I, %I',
                   runtime_role, market_runtime, market_owner);
    EXECUTE format('GRANT SELECT, INSERT ON public.users TO %I', runtime_role);
    EXECUTE format('GRANT SELECT, INSERT, UPDATE ON public.accounts, public.sessions TO %I', runtime_role);
    EXECUTE format('GRANT SELECT, UPDATE ON public.roles TO %I', runtime_role);
    EXECUTE format('GRANT SELECT, INSERT, DELETE ON public.user_roles, public.rotated_refresh_token_history TO %I', runtime_role);
    EXECUTE format('GRANT SELECT, INSERT ON public.auth_audit_events TO %I', runtime_role);
    EXECUTE format('GRANT USAGE ON SCHEMA public TO %I', market_owner);
    EXECUTE format('GRANT REFERENCES (id) ON public.users TO %I', market_owner);
END $$;
