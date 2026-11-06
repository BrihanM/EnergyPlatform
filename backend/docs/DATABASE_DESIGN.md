# PostgreSQL Database Design

## Design status

The database remains one PostgreSQL instance and one normalized relational
database. The tables below are proposed; this phase creates no migration or
table. Existing Auth tables are documented from Flyway migrations and remain
owned by `auth-service`.

## Ownership and existing tables

| Table | Owner/writer | Key constraints |
|---|---|---|
| `users` | `auth-service` | `id UUID PK`, required names and timestamps |
| `accounts` | `auth-service` | `id UUID PK`, `user_id FK`, unique normalized username/email, status check |
| `sessions` | `auth-service` | `id UUID PK`, `account_id FK`, 43-character refresh hash, expiry check |

The market service references `users.id`; it never duplicates user names or
account credentials. Auth must not be required to join every market query.

## Proposed tables

| Table | Main columns | PK/FK and constraints |
|---|---|---|
| `energy_offers` | `id`, `seller_user_id`, `energy_quantity_kwh`, `price_per_kwh`, `available_from`, `expires_at`, `status`, `published_at`, `updated_at` | PK `id`; FK seller -> `users.id`; positive quantity; non-negative price; `available_from < expires_at`; enum check |
| `energy_transactions` | `id`, `offer_id`, `seller_user_id`, `buyer_user_id`, `quantity_kwh`, `price_per_kwh`, `total_amount`, `status`, `created_at`, `closed_at`, `idempotency_key` | PK `id`; FKs offer/users; positive quantity/total; seller != buyer; terminal `closed_at` check; unique idempotency scope |
| `meter_devices` | `id`, `owner_user_id`, `device_identifier`, `credential_hash`, `status`, `registered_at` | PK `id`; FK owner -> `users.id`; unique device identifier; credential hash not null; status check |
| `energy_readings` | `id`, `meter_device_id`, `generated_energy_kwh`, `surplus_energy_kwh`, `measured_at`, `received_at`, `validation_status` | PK `id`; FK device; non-negative values; surplus <= generated; status check; duplicate policy index/constraint |
| `operator_verifications` | `id`, `transaction_id`, `result`, `verified_at`, `reason`, `simulation_reference` | PK `id`; FK transaction; result check; reference required; uniqueness if one verification per transaction |
| `audit_events` | `id`, nullable `user_id`, `event_type`, `occurred_at`, `source_ip`, `resource_type`, `resource_id`, `result`, `correlation_id`, safe `metadata` | PK `id`; optional FK user; no secrets; append-only policy |

`NUMERIC` is required for energy and simulated money; Java implementations must
use `BigDecimal`, never binary floating point. `TIMESTAMPTZ` maps to `Instant`.
UUID is consistent with the existing Auth identifiers.

## Recommended indexes

| Table | Index |
|---|---|
| `energy_offers` | `(status, expires_at, published_at)` for active listings |
| `energy_offers` | `(seller_user_id, updated_at)` for owner history |
| `energy_transactions` | `(buyer_user_id, created_at)` and `(seller_user_id, created_at)` |
| `energy_transactions` | unique `(buyer_user_id, idempotency_key)` when key is present |
| `energy_readings` | `(meter_device_id, measured_at)` |
| `operator_verifications` | `(transaction_id, verified_at)` |
| `audit_events` | `(occurred_at)`, `(user_id, occurred_at)`, `(resource_type, resource_id)` |

Partial indexes and JSONB metadata are acceptable only after query requirements
are confirmed. Audit metadata must be allow-listed and bounded to avoid using
the audit table as an unstructured data store.

## Referential and deletion policy

All business FKs should use `ON DELETE RESTRICT` or a logical deactivation
policy. Offers and transactions must not disappear when an account is disabled.
Devices can be revoked; readings and audit events are retained for traceability.
No cascade from a user deletion is recommended in this testbed.

## Atomic acquisition

The future repository adapter must execute acquisition in one PostgreSQL
transaction:

1. Lock the target offer row with `SELECT ... FOR UPDATE`.
2. Re-read status and expiry under the lock.
3. Verify buyer identity and seller/buyer constraint.
4. Insert the immutable transaction snapshot.
5. Move the offer to `SOLD` (or `RESERVED` only if a separate reservation
   protocol is approved).
6. Commit; any failure rolls back both rows.

An equivalent guarded update (`WHERE status = 'ACTIVE'`) may be used, but the
implementation must verify the affected-row count. A unique idempotency key
prevents client retries from creating duplicate transactions. A database
constraint remains the final concurrency barrier.

## Flyway and shared database decision

Current Auth migrations are `V1` through `V4` under Auth. Because both services
will target one database, independently starting a second `V1` sequence is unsafe.
Before implementation, approve one of:

- a single centrally ordered migration stream with explicit ownership;
- one database schema per bounded context inside the same database, with a
  coordinated migration history and deliberate cross-schema FKs; or
- a shared migration module executed once during deployment.

The chosen option must preserve the user's one-database decision and must not
silently allow both services to mutate the same tables.
