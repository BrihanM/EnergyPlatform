# Energy Offers Module

## Scope

F12 implements the `EnergyOffer` aggregate in `energy-market-service`. It supports
publication, public availability queries, owner queries, owner updates, and logical
cancellation. Transactions, reservations, purchases, meters, operator verification,
and durable business audit are intentionally out of scope.

The aggregate uses UUID identifiers and stores only `seller_id`; identity data remains
owned by Auth. The seller is always obtained from the Gateway's validated
`X-Authenticated-User-Id` header and never from the request body.

## Domain and persistence

Offers use the states `ACTIVE`, `RESERVED`, `SOLD`, `CANCELLED`, and `EXPIRED`.
F12 implements `ACTIVE`, `CANCELLED`, and `EXPIRED`; the other states are reserved
for the transaction module. Quantity and price must be positive, and an offer can
only be edited or cancelled while active and unexpired.

`V2__create_energy_offers.sql` creates `energy_market.energy_offers` with:

- UUID primary key and a foreign key to `public.users(id)`.
- `NUMERIC(12,3)` energy quantity and `NUMERIC(14,2)` price per kWh.
- `TIMESTAMPTZ` lifecycle timestamps.
- Database checks for positive values, valid states, and creation/expiry ordering.
- Status/expiry, seller/update, and price indexes.
- A `version` column used by guarded optimistic updates.

The price scale is two decimal places because this testbed treats the value as a
simulated unit price rather than a calculated settlement total. Java uses
`BigDecimal` throughout.

## Hexagonal structure

- `domain`: aggregate, lifecycle enum, and business exception; no Spring or JDBC.
- `application`: input/output ports, query/page models, and use-case orchestration.
- `infrastructure.adapter.in.web`: REST DTOs, identity extraction, controller, and
  uniform HTTP errors.
- `infrastructure.adapter.out.postgresql`: `NamedParameterJdbcTemplate` repository.

Updates and cancellations use a transaction and a guarded `version`, seller, active
status, and non-expired predicate. A concurrent change therefore returns a conflict
instead of silently overwriting state.

## HTTP API

The canonical paths are:

| Method | Path | Access |
|---|---|---|
| POST | `/api/v1/offers` | `NORMAL_USER` |
| GET | `/api/v1/offers` | Public |
| GET | `/api/v1/offers/{id}` | Public only while available |
| GET | `/api/v1/offers/me` | `NORMAL_USER` |
| PUT | `/api/v1/offers/{id}` | Owning `NORMAL_USER` |
| DELETE | `/api/v1/offers/{id}` | Owning `NORMAL_USER` |

The equivalent `/api/v1/market/offers` prefix is also accepted for compatibility
with the reserved market route. Gateway forwards both prefixes internally to the
same service; the service port is not published by Compose.

List queries use bounded `page` (default `0`) and `size` (default `20`, maximum `100`)
parameters. Public filters include `minEnergy`, `maxEnergy`, `minPrice`,
`maxPrice`, `expiresBefore`, and UUID `id`. Allowed sort fields are `createdAt`,
`expiresAt`, `pricePerKwh`, and `energyAmountKwh`. Invalid ranges and sort values
return `400`.

Public queries select only `ACTIVE` rows with `expires_at > CURRENT_TIMESTAMP`.
Before list queries, expired active rows are materialized as `EXPIRED`; detail and
write operations recheck expiry under the database transaction.

## Authorization and errors

Only the exact trusted role `NORMAL_USER` authorizes write operations. `ADMIN` and
`SECURITY_AUDITOR` do not gain offer permissions merely from their roles. Ownership
is checked against the authenticated user UUID in the service, not by a client
field. Missing/invalid identity returns `401`, insufficient role returns `403`, and
an absent or foreign offer returns `404` to avoid IDOR disclosure. State conflicts
return `409`; validation errors return `400`.

Gateway removes client-supplied identity headers before rebuilding them from a valid
JWT. Energy Market is attached only to the internal Docker network, so these headers
are trusted only on that network boundary and are not an external authentication
mechanism.

## Testing

The module includes domain invariant tests, REST authorization tests, ArchUnit
dependency tests, Spring context tests, and a PostgreSQL Testcontainers migration
test. The PostgreSQL test verifies the Auth user reference, Flyway version `2`,
the offers table, and the absence of unrelated business tables.
