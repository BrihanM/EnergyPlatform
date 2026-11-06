# Transactions module

F14 adds synchronous purchase processing to `energy-market-service`. A normal
user submits one `POST /api/v1/transactions` request with `{ "offerId": "..." }`
and an `Idempotency-Key` header. The buyer is taken only from the Gateway
identity headers; seller, quantity and price are read from the locked offer.

## Domain and consistency

`EnergyTransaction` stores an immutable economic snapshot: energy (three
decimals), unit price (two decimals), COP currency, and total calculated with
`BigDecimal` and `HALF_UP` at two decimals. Its states are
`PENDING_VERIFICATION`, `CONFIRMED`, `REJECTED`, and `CANCELLED`.

The purchase use case locks the offer with PostgreSQL `FOR UPDATE`, validates
expiry, ownership and state, claims the buyer/key pair, changes the offer to
`RESERVED`, runs the local deterministic simulator, persists an
`operator_verifications` row, then changes the offer to `SOLD`, `ACTIVE`, or
`EXPIRED`. Functional rejection is persisted and is not an HTTP 500. Technical
failures roll the whole operation back.

The `transaction_idempotency_keys` table makes concurrent retries safe. A
replay with the same buyer, key and offer returns the original transaction; the
same key for another offer is a conflict. Keys are 1-100 characters and use
letters, digits, dot, underscore, colon or hyphen.

## Simulation and verification

`EnergyTransferSimulator` is an outbound port. The local adapter is configured
with `ENERGY_MARKET_SIMULATION_MODE=SUCCESS|REJECT|RULE_BASED` and defaults to
`SUCCESS`. It produces a deterministic decision and a unique simulation
reference without calling a grid operator or performing physical transfer.
The operator verification is persisted as `ACCEPTED` or `REJECTED` with a
non-sensitive reason and reference.

## API

- `POST /api/v1/transactions` (`NORMAL_USER`): automatic purchase.
- `GET /api/v1/transactions/me` (`NORMAL_USER`): own buyer/seller history,
  with `size`, `offset`, `status`, `from`, and `to` filters.
- `GET /api/v1/transactions/{id}` (`NORMAL_USER`): participant-only detail.

There is deliberately no manual confirmation or transfer endpoint.

## Persistence

Migration `V3__create_energy_transactions.sql` owns
`energy_transactions`, `operator_verifications`, and
`transaction_idempotency_keys` in `energy_market`. Auth owns `public.users`;
the market service writes only its own schema. PostgreSQL foreign keys,
checks, unique constraints, indexes, and row locks are the consistency
authority.
