# Business Flows

These flows describe current behavior. Publication and consultation are
implemented by F12; automatic transactions and verification are implemented by
F14.

## Publication

1. An authenticated user submits quantity, price per kWh, and validity period.
2. The application service derives `sellerUserId` from the validated identity,
   validates numeric ranges and dates, and creates an `ACTIVE` offer.
3. The service persists the offer and emits a safe audit event.
4. The owner may edit only mutable fields while the offer is `ACTIVE`; edit
   history and updated timestamp are retained.
5. The owner may cancel an `ACTIVE` offer. Expiry is derived from `expiresAt`
   and produces `EXPIRED`; sold/cancelled offers are terminal.

## Consultation

F12 exposes public visibility. The query returns only active, non-expired offers,
supports bounded pagination, allow-listed sorting and numeric/date filters, and
never exposes credentials or internal error details. A read does not reserve an
offer.

## Atomic purchase

1. Gateway authenticates the request and provides the caller identity.
2. The service validates the offer ID, optional idempotency key, and caller.
3. One database transaction locks the offer row.
4. It rechecks `ACTIVE` status and expiry after the lock.
5. It rejects self-purchase and invalid quantity.
6. It calculates the total from the stored unit price using `BigDecimal`.
7. It inserts a `PENDING_VERIFICATION` transaction containing seller, buyer,
   quantity, unit price, and total snapshots.
8. It runs the deterministic transfer simulator and persists operator verification.
9. It confirms the transaction and changes the offer to `SOLD`, or rejects it and
   returns the offer to `ACTIVE`/`EXPIRED`.
10. It commits. Any technical error rolls back all writes.

Two concurrent buyers cannot both acquire the row: one transaction obtains the
row lock and commits `SOLD`; the other rechecks status and receives a conflict.
The idempotency key handles retries after network ambiguity. Do not rely on a
frontend check or Redis lock as the correctness mechanism.

## Simulated verification and closure

1. A transaction in `PENDING_VERIFICATION` is passed to the simulated operator
   adapter.
2. The adapter evaluates deterministic test rules (for example, configured
   acceptance, reading consistency, or a test scenario) and returns a
   `simulationReference`.
3. The service persists `OperatorVerification`.
4. Accepted verification changes the transaction to `CONFIRMED`; rejection
   changes it to `REJECTED`. `closedAt` is set only for terminal outcomes.
5. The transition and result are audited. Reprocessing must be idempotent and
   must not create conflicting terminal states.

There is no payment, physical meter command, utility API, or real energy
settlement. The simulation remains durable and traceable.

## Device readings

1. A device adapter authenticates an opaque device credential independently of
   user JWT sessions.
2. It verifies the device is `ACTIVE`, validates the payload and timestamps,
   and persists a `PENDING` reading.
3. Domain validation marks it `VALID`, `INCONSISTENT`, or `REJECTED` with a
   reason suitable for audit.
4. Readings are immutable measurements; corrections are new records, not silent
   updates. Credentials and raw secrets are never persisted or audited.

## Audit

Every command and security-relevant outcome carries the Gateway correlation ID
when available. The event contains actor UUID, event type, resource reference,
result, timestamp, source IP when trustworthy, and safe metadata. It excludes
passwords, access tokens, refresh tokens, device credentials, request bodies,
and SQL. Query access is restricted to the security auditor role; retention and
redaction policy require approval.

## Failure and idempotency rules

Validation failures do not write business state unless a rejected attempt is
explicitly audited. Conflicts return a stable application error. Transactional
writes are rolled back together. External simulation calls must use a stable
transaction/reference key or an outbox/retry policy before asynchronous
processing is introduced.
