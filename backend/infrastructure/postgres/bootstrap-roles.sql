\getenv auth_runtime AUTH_DB_USERNAME
\getenv auth_runtime_password AUTH_DB_PASSWORD
\getenv auth_owner AUTH_FLYWAY_USERNAME
\getenv auth_owner_password AUTH_FLYWAY_PASSWORD
\getenv market_runtime ENERGY_MARKET_DB_USERNAME
\getenv market_runtime_password ENERGY_MARKET_DB_PASSWORD
\getenv market_owner ENERGY_MARKET_FLYWAY_USERNAME
\getenv market_owner_password ENERGY_MARKET_FLYWAY_PASSWORD
\getenv market_schema ENERGY_MARKET_DB_SCHEMA

BEGIN;
SELECT 1 / CASE WHEN :'market_schema' = 'energy_market' THEN 1 ELSE 0 END;
SELECT 1 / CASE WHEN count(DISTINCT name) = 4
                     AND bool_and(name ~ '^[a-z_][a-z0-9_]{0,62}$')
                     AND bool_and(name <> current_user)
                THEN 1 ELSE 0 END
FROM (VALUES (:'auth_runtime'), (:'auth_owner'), (:'market_runtime'), (:'market_owner')) AS roles(name);

SELECT 1 / CASE WHEN EXISTS (
    SELECT 1 FROM pg_roles r
    WHERE r.rolname IN (:'auth_runtime', :'auth_owner', :'market_runtime', :'market_owner')
      AND (r.rolsuper OR r.rolcreatedb OR r.rolcreaterole OR r.rolreplication OR r.rolbypassrls
           OR EXISTS (SELECT 1 FROM pg_auth_members m WHERE m.member = r.oid))
) THEN 0 ELSE 1 END;

SELECT format('CREATE ROLE %I LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS NOINHERIT PASSWORD %L',
              name, password)
FROM (VALUES (:'auth_runtime', :'auth_runtime_password'), (:'auth_owner', :'auth_owner_password'),
             (:'market_runtime', :'market_runtime_password'), (:'market_owner', :'market_owner_password')) AS roles(name, password)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = name)
\gexec

SELECT format('ALTER ROLE %I LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS NOINHERIT PASSWORD %L',
              name, password)
FROM (VALUES (:'auth_runtime', :'auth_runtime_password'), (:'auth_owner', :'auth_owner_password'),
             (:'market_runtime', :'market_runtime_password'), (:'market_owner', :'market_owner_password')) AS roles(name, password)
\gexec

SELECT 1 / CASE WHEN EXISTS (
    SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_roles r ON r.oid = c.relowner
    WHERE n.nspname = 'public'
      AND c.relname IN ('users', 'accounts', 'sessions', 'roles', 'user_roles',
                        'auth_audit_events', 'rotated_refresh_token_history', 'flyway_schema_history')
      AND c.relkind IN ('r', 'p') AND r.rolname <> :'auth_owner'
) THEN 0 ELSE 1 END;
SELECT 1 / CASE WHEN EXISTS (
    SELECT 1 FROM pg_namespace n JOIN pg_roles r ON r.oid = n.nspowner
    WHERE n.nspname = 'energy_market' AND r.rolname <> :'market_owner'
) THEN 0 ELSE 1 END;
SELECT 1 / CASE WHEN EXISTS (
    SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_roles r ON r.oid = c.relowner
    WHERE n.nspname = 'energy_market'
      AND c.relkind IN ('r', 'p', 'S', 'v', 'm', 'f')
      AND r.rolname <> :'market_owner'
) THEN 0 ELSE 1 END;

SELECT format('GRANT USAGE, CREATE ON SCHEMA public TO %I', :'auth_owner') \gexec
SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'auth_runtime') \gexec
SELECT format('CREATE SCHEMA energy_market AUTHORIZATION %I', :'market_owner')
WHERE NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'energy_market') \gexec
SELECT format('GRANT USAGE ON SCHEMA energy_market TO %I', :'market_runtime') \gexec
-- Created here as superuser so the Auth owner does not need database-level CREATE; V8 is then a no-op for the extension.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
COMMIT;
