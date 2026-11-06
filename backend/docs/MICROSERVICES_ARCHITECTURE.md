# Microservices Architecture

## Current baseline

The repository is a Maven monorepo with `api-gateway` and `auth-service`.
Frontend traffic enters the Gateway, which routes internally, applies CORS,
correlation IDs, security headers, and validates the JWT signature, issuer,
expiry, UUID `sub`, and `username`. Auth owns registration, login, refresh
rotation, logout, `users`, `accounts`, and `sessions`.

Docker Compose currently exposes only the Gateway to the host. PostgreSQL and
Redis are on `backend-internal`; Auth is internal. No change to this topology is
part of this design phase.

## Target service boundaries

```text
frontend
   |
api-gateway
   |-- /api/v1/auth/**  -> auth-service
   |-- /api/v1/market/** -> energy-market-service (future)
   |-- perimeter controls, correlation, technical audit
   |
   +-- auth-service
   |     +-- users, accounts, sessions
   |
   +-- energy-market-service
         +-- offers
         +-- transactions
         +-- readings/devices
         +-- simulated verification

one PostgreSQL database + Redis infrastructure
```

`energy-market-service` is the recommended initial boundary. Offers, transactions,
readings, and verification share business invariants and a simulated workflow;
splitting them into one service per entity would create premature distributed
coordination. Internal modules preserve cohesion and allow later extraction.

## Proposed package structure

Adapt names to the existing `com.miapp...` convention when implementation starts:

```text
backend/services/energy-market-service/
  src/main/java/com/miapp/energy/
    domain/
      model/
        offers/
        transactions/
        readings/
        verification/
        audit/
      enums/
      exception/
    application/
      port/
        in/
        out/
      service/
      command/
      query/
      result/
    infrastructure/
      adapter/
        in/web/
        out/persistence/
        out/integrations/
      config/
  src/main/resources/db/migration/
```

Domain objects and rules must remain free of Spring, JPA, HTTP, Redis, and JWT
types. Application services coordinate use cases through input/output ports.
REST controllers, JPA repositories, Flyway, device authentication, and the
simulated operator are adapters. Persistence entities must be separate from
domain entities, as in Auth.

## Responsibilities and dependencies

| Component | Owns | Must not do |
|---|---|---|
| Gateway | routing, JWT validation, CORS, correlation, perimeter metadata | market rules or direct database writes |
| Auth | identity lifecycle and account/session persistence | offer/transaction records or duplicated profiles |
| Energy market | offer and transaction invariants, device/readings, simulation orchestration | password/token management or direct Auth writes |
| PostgreSQL | relational integrity and durable state | become an implicit cross-service API |
| Redis | available infrastructure for future rate limiting/cache | be the source of truth for transactions |

The future market service can trust the Gateway's validated identity only when it
is reachable exclusively through the internal route. It should still derive the
caller from the forwarded identity contract and never accept a user ID in the
request body as the authority.

## Shared database consequences

There is one normalized PostgreSQL database, not one database per service.
Auth owns writes to `users`, `accounts`, and `sessions`. The market service owns
its proposed tables and may reference `users.id` with foreign keys, but must not
update or delete Auth rows. `ON DELETE RESTRICT` is recommended for business
records so account removal cannot erase history. This arrangement requires:

1. A coordinated Flyway versioning/ownership policy because two application
   migration directories target one database.
2. Explicit schema/table ownership and review of cross-service migrations.
3. Backward-compatible migrations and deployment ordering.
4. No ORM relationship graph crossing service boundaries; use UUID references.

The migration coordination mechanism is a pending decision; no migration is
created here.

## Error, observability, and security contracts

The market service should follow Auth's safe error envelope and preserve
`X-Correlation-ID`. Validation errors, authorization errors, conflicts, and
unexpected failures must remain distinguishable by status/code without leaking
SQL or credentials. All business state transitions emit an allow-listed audit
event. Rate limiting can use Redis later, but authorization remains based on
validated identity and explicit role claims/lookup.

## Implementation sequencing (future)

1. Agree on roles and the shared Flyway policy.
2. Create the service skeleton and domain tests without endpoints.
3. Add migrations and persistence adapters.
4. Implement offer lifecycle and read-only listing.
5. Implement atomic acquisition and transaction history.
6. Add simulated readings and verification.
7. Add audit persistence/query and frontend features.
