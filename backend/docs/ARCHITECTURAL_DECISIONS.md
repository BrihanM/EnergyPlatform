# Architectural Decisions

## ADR-001: One market service for the initial bounded context

**Decision:** Keep offers, transactions, readings, and simulated verification
inside `energy-market-service`, separated by internal modules.

**Rationale:** These capabilities share the purchase/verification workflow and
the same PostgreSQL transaction boundary. One service preserves ACID behavior and
keeps the testbed understandable. Separate services per entity would introduce
distributed transactions and messaging before the domain requires them.

**Revisit when:** independent scaling, ownership, deployment cadence, or
security boundaries justify extraction.

## ADR-002: One PostgreSQL database, explicit table ownership

**Decision:** Preserve the defined single PostgreSQL instance/database. Auth
owns and writes `users`, `accounts`, and `sessions`; the market service owns its
business tables. Cross-context UUID FKs are allowed because the database is
shared, but no service may write another service's tables.

**Trade-off:** Referential integrity is strong, while service independence is
reduced. Deployment and Flyway history require coordination.

## ADR-003: UUID identity references, no User duplication

**Decision:** Market records store `UUID` user references and do not duplicate
names, emails, passwords, roles, JWTs, or refresh tokens.

**Rationale:** This matches current Auth migrations/entities and minimizes
personal-data duplication. Read models can be added later if cross-service
display data needs caching.

## ADR-004: Price per kWh plus immutable transaction snapshot

**Decision:** `EnergyOffer` stores `pricePerKwh`; the initial purchase acquires
the full quantity. `EnergyTransaction` stores quantity, unit price, seller,
buyer, and calculated total.

**Rationale:** Unit pricing is unambiguous for energy quantities and supports a
future partial-purchase decision. The snapshot protects historical integrity
from later offer edits. `BigDecimal`/PostgreSQL `NUMERIC` are mandatory.

## ADR-005: Database locking is the purchase concurrency authority

**Decision:** Acquire an offer in one PostgreSQL transaction using a row lock or
a guarded update plus affected-row verification, then insert the transaction
and update offer state atomically.

**Rationale:** This prevents double acquisition under concurrent requests.
Frontend checks, process-local locks, and Redis locks are not correctness
mechanisms. Idempotency keys protect retries.

## ADR-006: Simulated devices and operator

**Decision:** Device credentials and operator verification are explicit domain
records/adapters, but neither integrates with a real utility or represents
physical energy.

**Rationale:** The testbed needs repeatable, auditable security and business
flows. Device authentication is separate from user JWT authentication, and
simulation references make test outcomes traceable.

**F16 implementation:** ADMIN provisions an independent device credential;
Market stores its SHA-256 hash and verifies it under a row lock. Device UUIDs
and credentials do not confer user roles. Reading UUIDs are supplied by devices,
duplicates are rejected, and an excess surplus is retained as `INCONSISTENT`.
TLS, rate limiting, secure delivery and durable audit remain operational work.

## ADR-007: Roles are explicit and least-privilege

**Decision:** Adopt the conceptual roles `NORMAL_USER`, `ADMIN`, and
`SECURITY_AUDITOR`, subject to approval and implementation in Auth.

| Operation | NORMAL_USER | ADMIN | SECURITY_AUDITOR |
|---|---:|---:|---:|
| Publish/cancel own offer | Yes | Yes, subject to policy | No |
| Browse offers | Yes | Yes | Read-only |
| Buy an offer | Yes | No by default | No |
| View own history | Yes | Authorized support access | Read-only when audit scope permits |
| Manage/suspend users | No | Yes | No |
| Query audit records | No | No by default | Yes |
| Ingest own device readings | Yes, via device credential | Operationally controlled | No |

Auth persists the official roles and includes them in JWT claims. They are
independent of `AccountStatus`; device credentials do not grant any user role.

### F17 security design question, before implementation

The current Gateway validates JWT signature, issuer, expiration and embedded
roles locally; it does not check the current account status or role assignments.
Revoking refresh sessions alone therefore cannot immediately invalidate an
already-issued access token. A suspended user or a user whose role was removed
can continue using that token until its expiry, including against Market.
One alternative is a fail-closed, authenticated Gateway-to-Auth validation on
protected requests that checks current account and role state. It provides
immediate enforcement but adds latency and makes Auth availability a dependency
of protected traffic. The alternative is to retain the expiry-bound window and
document that risk; this does not provide immediate account or role revocation.
The deployment and availability trade-off requires a decision before changing
the authentication boundary.

## Decisions requiring approval before implementation

1. Further role-change audit and operational controls for admin bootstrap.
2. Shared Flyway migration strategy for two services writing one database.
3. Whether offer consultation is public or requires authentication.
4. Whether purchases are always full-offer or allow partial quantities.
5. Expiry mechanism (scheduled job, query-time derivation, or both).
6. Durable audit owner and retention/redaction policy.
7. Audit metadata column format and maximum size.
8. Device rate limits, secure delivery and operational key management.
9. Simulated operator rule set, retry policy, and verification cardinality.
10. Decimal scale/rounding and any simulated currency unit.
11. Normative framework and evidence required for HU-25.

## Out of scope for this phase

No service, Java class, endpoint, migration, Docker service, dependency, React
component, RBAC implementation, or business logic is added by this design.
