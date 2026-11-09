package com.miapp.auth;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class PostgresRuntimePrivilegesIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6-alpine3.24");
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String AUTH_OWNER = "test_auth_owner";
    private static final String AUTH_RUNTIME = "test_auth_runtime";
    private static final String MARKET_OWNER = "test_market_owner";
    private static final String MARKET_RUNTIME = "test_market_runtime";

    @BeforeAll
    static void migrateWithSeparateOwners() throws Exception {
        try (Connection admin = DriverManager.getConnection(POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(), POSTGRES.getPassword()); Statement sql = admin.createStatement()) {
            for (String role : new String[]{AUTH_OWNER, AUTH_RUNTIME, MARKET_OWNER, MARKET_RUNTIME}) {
                sql.execute("CREATE ROLE " + role + " LOGIN NOINHERIT PASSWORD '" + PASSWORD + "'");
            }
            sql.execute("GRANT CREATE ON DATABASE " + POSTGRES.getDatabaseName() + " TO " + AUTH_OWNER);
            sql.execute("GRANT USAGE, CREATE ON SCHEMA public TO " + AUTH_OWNER);
            sql.execute("GRANT USAGE ON SCHEMA public TO " + AUTH_RUNTIME);
            sql.execute("CREATE SCHEMA energy_market AUTHORIZATION " + MARKET_OWNER);
            sql.execute("GRANT USAGE ON SCHEMA energy_market TO " + MARKET_RUNTIME);
        }
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), AUTH_OWNER, PASSWORD)
                .locations("classpath:db/migration", "classpath:db/pg-grants")
                .placeholders(Map.of("authRuntimeRole", AUTH_RUNTIME, "piiEncryptionKey", "test-only-pii-key-not-for-production", "marketMigrationRole", MARKET_OWNER,
                        "marketRuntimeRole", MARKET_RUNTIME))
                .load().migrate();
        Path marketDb = Path.of("..", "energy-market-service", "src", "main", "resources", "db")
                .toAbsolutePath().normalize();
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), MARKET_OWNER, PASSWORD)
                .schemas("energy_market").defaultSchema("energy_market")
                .locations("filesystem:" + marketDb.resolve("migration"), "filesystem:" + marketDb.resolve("pg-grants"))
                .placeholders(Map.of("marketRuntimeRole", MARKET_RUNTIME, "authRuntimeRole", AUTH_RUNTIME))
                .load().migrate();
    }

    @Test
    void runtimeRolesCanOnlyAppendAndReadTheirOwnAudit() throws Exception {
        try (Connection auth = connection(AUTH_RUNTIME); Connection market = connection(MARKET_RUNTIME);
             Statement a = auth.createStatement(); Statement m = market.createStatement()) {
            a.executeUpdate("""
                    INSERT INTO public.auth_audit_events
                    (id,event_type,occurred_at,actor_type,resource_type,result)
                    VALUES (gen_random_uuid(),'LOGIN_FAILED',now(),'ANONYMOUS','ACCOUNT','REJECTED')
                    """);
            m.executeUpdate("""
                    INSERT INTO energy_market.audit_events
                    (id,event_type,occurred_at,actor_type,actor_id,resource_type,resource_id,result)
                    VALUES (gen_random_uuid(),'OFFER_CREATED',now(),'USER',gen_random_uuid(),
                            'OFFER',gen_random_uuid(),'SUCCESS')
                    """);
            assertThat(count(a, "SELECT count(*) FROM public.auth_audit_events")).isEqualTo(1);
            assertThat(count(m, "SELECT count(*) FROM energy_market.audit_events")).isEqualTo(1);
            for (String command : new String[]{
                    "UPDATE public.auth_audit_events SET result = 'SUCCESS'",
                    "DELETE FROM public.auth_audit_events",
                    "TRUNCATE public.auth_audit_events",
                    "ALTER TABLE public.auth_audit_events ADD COLUMN unwanted int",
                    "DROP TABLE public.auth_audit_events",
                    "SELECT * FROM energy_market.audit_events",
                    "UPDATE energy_market.energy_offers SET status = 'ACTIVE'",
                    "CREATE TABLE public.runtime_test_table (id int)",
                    "SET ROLE " + AUTH_OWNER}) {
                denied(a, command);
            }
            for (String command : new String[]{
                    "UPDATE energy_market.audit_events SET result = 'SUCCESS'",
                    "DELETE FROM energy_market.audit_events",
                    "TRUNCATE energy_market.audit_events",
                    "ALTER TABLE energy_market.audit_events ADD COLUMN unwanted int",
                    "DROP TABLE energy_market.audit_events",
                    "SELECT * FROM public.auth_audit_events",
                    "UPDATE public.accounts SET status = 'ACTIVE'",
                    "CREATE TABLE energy_market.runtime_test_table (id int)",
                    "SET ROLE " + MARKET_OWNER}) {
                denied(m, command);
            }
        }
    }

    @Test
    void futureTablesDefaultToNoRuntimePrivileges() throws Exception {
        try (Connection authOwner = connection(AUTH_OWNER); Connection marketOwner = connection(MARKET_OWNER);
             Connection authRuntime = connection(AUTH_RUNTIME); Connection marketRuntime = connection(MARKET_RUNTIME);
             Statement ao = authOwner.createStatement(); Statement mo = marketOwner.createStatement();
             Statement a = authRuntime.createStatement(); Statement m = marketRuntime.createStatement()) {
            ao.execute("CREATE TABLE public.future_auth_audit (id int)");
            mo.execute("CREATE TABLE energy_market.future_audit (id int)");
            ao.execute("CREATE SEQUENCE public.future_auth_sequence");
            mo.execute("CREATE SEQUENCE energy_market.future_sequence");
            denied(a, "SELECT * FROM public.future_auth_audit");
            denied(a, "INSERT INTO public.future_auth_audit VALUES (1)");
            denied(a, "SELECT nextval('public.future_auth_sequence')");
            denied(m, "SELECT * FROM energy_market.future_audit");
            denied(m, "INSERT INTO energy_market.future_audit VALUES (1)");
            denied(m, "SELECT nextval('energy_market.future_sequence')");
        }
    }

    private static Connection connection(String role) throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), role, PASSWORD);
    }

    private static int count(Statement sql, String query) throws SQLException {
        try (var rows = sql.executeQuery(query)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private static void denied(Statement sql, String command) {
        assertThatThrownBy(() -> sql.execute(command)).isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState()).isEqualTo("42501");
    }
}
