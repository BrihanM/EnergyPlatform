# Energy Market Service

## Responsibility and scope

`energy-market-service` owns the energy-market bounded context:
offers, transactions, authenticated meter readings, simulated operator
verification, and business audit records. F11 creates infrastructure only; no
business entity, transaction workflow is implemented. F12 adds the persisted EnergyOffer
publication and consultation slice.

The modules remain inside one service because these capabilities share market
invariants and a simulated workflow. They can be extracted later if their
runtime or ownership boundaries justify it.

## Hexagonal structure

The service uses the same Maven and package conventions as Auth:

```text
services/energy-market-service/
  src/main/java/com/miapp/energy/
    domain/          # framework-free business model boundary
    application/    # future use cases and ports
    infrastructure/ # configuration and adapters
```

The dependency direction is `infrastructure -> application -> domain`.
Package-level architecture tests prevent domain dependencies on Spring/JPA and
application dependencies on infrastructure.

## Runtime and Gateway

The service listens on port `8082` by default. The Gateway forwards `/api/v1/offers/**` and the compatibility
`/api/v1/market/**` route to `ENERGY_MARKET_SERVICE_URI`. In Compose the destination is
`http://energy-market-service:8082`.

Only the Gateway is published to the host. Energy Market is attached only to
the internal Docker network and has no host port mapping. Requests reaching
future endpoints through the Gateway will contain the trusted F10 contract:

| Header | Meaning |
|---|---|
| `X-Authenticated-User-Id` | UUID from the validated JWT subject |
| `X-Authenticated-Username` | Username from the validated JWT |
| `X-Authenticated-Roles` | Comma-separated validated official roles |
| `X-Correlation-ID` | Correlation identifier |

These headers are not accepted as external authority. Future adapters must
enforce role and resource ownership in the service; the Gateway is not the
only authorization layer. Energy Market does not reimplement Auth JWT
validation.

## PostgreSQL and Flyway ownership

The service uses the same PostgreSQL database and credentials as Auth but owns
the `energy_market` schema. Auth remains owner of the public schema and its
`flyway_schema_history`. Energy Market configures:

- schema: `ENERGY_MARKET_DB_SCHEMA` (default `energy_market`)
- migrations: `classpath:db/migration`
- independent history: `energy_market.flyway_schema_history`
- `ddl-auto` is not used; F12 uses JDBC persistence rather than JPA

`V1__initialize_energy_market_schema.sql` initializes the schema,
`V2__create_energy_offers.sql` creates the F12 `energy_offers` table with a
foreign key to `public.users`, lifecycle checks, indexes, and an optimistic
locking version, and `V3__create_energy_transactions.sql` creates transaction,
verification, and idempotency tables. Future phases add only Energy Market
tables and must not update or delete Auth-owned rows.

## Configuration

Important variables are documented in `.env.example`:

| Variable | Default | Purpose |
|---|---|---|
| `ENERGY_MARKET_SERVER_PORT` | `8082` | Internal HTTP port |
| `ENERGY_MARKET_DB_URL` | local shared DB URL | PostgreSQL JDBC URL |
| `ENERGY_MARKET_DB_USERNAME` / `PASSWORD` | shared local values | DB credentials |
| `ENERGY_MARKET_DB_SCHEMA` | `energy_market` | Owned PostgreSQL schema |
| `ENERGY_MARKET_FLYWAY_ENABLED` | `true` | Own Flyway lifecycle |
| `ENERGY_MARKET_LOG_LEVEL` | `INFO` | Service log level |
| `ENERGY_MARKET_SIMULATION_MODE` | `SUCCESS` | Local simulation mode: `SUCCESS`, `REJECT`, or `RULE_BASED` |
| `ENERGY_MARKET_SERVICE_URI` | `http://localhost:8082` | Gateway local destination |

No secrets are committed. Compose overrides the JDBC URL and Gateway URI with
internal Docker hostnames.

## Health and observability

Actuator exposes only `/actuator/health` and hides component details. The
database health indicator verifies PostgreSQL availability. Compose waits for
PostgreSQL health before starting the service and checks the service health
endpoint without publishing the service port.

`CorrelationIdFilter` preserves the Gateway correlation ID and places it in
MDC under `correlationId`; a value is generated only for internal diagnostics
when no header exists. Logs never include JWTs, passwords, refresh tokens, or
device credentials.

## Tests

- `ArchitectureTest` verifies the initial dependency direction.
- `EnergyMarketServiceApplicationTests` verifies Spring context startup with a
  test profile.
- `PostgresMigrationIntegrationTest` uses PostgreSQL Testcontainers when Docker
  is available and verifies the isolated schema/history and offer table.
- `EnergyOfferDomainTest` verifies aggregate invariants and terminal states.
- `EnergyOfferControllerAuthorizationTest` verifies the role boundary for
  write operations.
- `EnergyTransactionDomainTest` verifies money snapshots, precision, and
  self-purchase rejection.

Run from `backend`:

```powershell
.\mvnw.cmd -pl services/energy-market-service test
docker compose config --quiet
```
