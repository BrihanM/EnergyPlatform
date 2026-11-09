# Persistent audit and user administration

F17.2 is in progress. The endpoints and persistence described here require
further security, concurrency and cross-service integration validation before
acceptance; see the traceability matrix for the current status.

## Ownership and events

Auth owns `auth_audit_events` (Flyway V6); Market owns
`energy_market.audit_events` (Flyway V5). Each stores a UUID, UTC timestamp,
controlled event type, actor type and identifier, affected user where relevant,
resource and result. Auth additionally records the affected role. Events do not
store credentials, token values, password hashes, request bodies or device
secrets. No trusted source IP is currently available. Gateway's technical
events are operational telemetry, not these durable business records.

Auth records successful and rejected logins, refresh, logout, role changes
(including the initial administrator assignment as a system event) and account
suspension/reactivation. Market records offer changes, purchase start
and outcome, device provisioning, credential rotation and lifecycle changes,
and inconsistent readings. A rejected login is recorded in a new transaction
after the authentication transaction has rolled back and released its connection;
successful business events share the business transaction. Replays do not add
purchase events, and rolled-back operations do not record successful outcomes.

There are no event mutation or deletion APIs. This is not a tamper-proof audit
store: Compose's database owner and runtime user can alter records. Production
requires a separate Flyway owner and runtime role with only `SELECT` and
`INSERT` on audit tables, plus retention, backups and transport protection.
Those privileges are not configured or enforced by the current Compose setup.
Even with restricted runtime privileges, the database owner can alter records.

## Queries

`GET /api/v1/auth/audit/events` and `GET /api/v1/market/audit/events` are
separate queries requiring `SECURITY_AUDITOR`; `ADMIN` alone and `NORMAL_USER`
are forbidden. Both require `page` (zero-based) and `size` (1-100); optional
`from`, `to` (ISO-8601 instants), `user` (actor or affected user UUID) and
`type` filter the selected service's events. Results are ordered by timestamp
and ID descending and include `items`, `page`, `size`, and `hasNext`. Service
selection is by endpoint. Requests must traverse Gateway; direct backend access
must be restricted to trusted services.

## Administration

`GET /api/v1/admin/users?page=0&size=20` requires `ADMIN` and returns profile,
role, status and registration information without credentials. An administrator
can call `POST /api/v1/admin/users/{id}/suspension` or `/reactivation`.
Suspension changes `ACTIVE` to `LOCKED`, revokes all sessions and records the
actor in one transaction. Reactivation changes `LOCKED` to `ACTIVE` without
restoring sessions. `DISABLED` is not reactivated by this operation. Last-active
administrator checks and role mutations share a database lock. Login and refresh
lock the account before session creation or rotation. New assignments cannot
combine `ADMIN` and `SECURITY_AUDITOR`. Existing combined assignments remain
unchanged, but online authorization rejects them until an operator reviews and
removes the inappropriate role through a controlled, audited process. If the
only administrator has both roles, recovery requires an operator; do not remove
either role automatically. A production inventory has not been performed.

Gateway continues to validate each protected request online through Auth without
a cached authorization fallback. Auth retains hashes of rotated refresh tokens
in `rotated_refresh_token_history` (V7), associated with the owning session.
Logout can use a retained hash solely to revoke that session; refresh accepts
only the current hash. The cookie is a bearer credential: anyone possessing it
can revoke the associated session. A missing, unknown or expired historical
cookie returns 401 rather than a misleading 204. Repeated logout of an
identifiable revoked session returns 204. Hashes of tokens rotated before V7
cannot be recovered.

History remains usable until the session expires, including sliding extensions
on refresh. An hourly cleanup removes histories for up to 500 expired sessions
per run, locking session rows with `SKIP LOCKED`; backlog can delay deletion.
The cleanup interval can be set with `security.refresh-token.history-cleanup-interval`.
Long-lived sessions and frequent rotation can accumulate history until expiry,
so production needs rate limiting and storage monitoring. No raw refresh token
or client-supplied session identifier is stored or trusted for logout.

## Validation status

`mvn verify` passed for Gateway (43 tests), Auth (40) and Market (30), with
PostgreSQL-backed tests and Flyway migrations. A subsequent Auth run passed
43 tests, including suspension rollback, concurrent suspension of two
administrators and the bootstrap role guard.
An isolated Compose stack exercised real Gateway, Auth, Market and PostgreSQL:
public offers, forged identity rejection, role changes, suspension, old JWT and
refresh rejection, reactivation, fresh login, and auditor-only queries from
both services. This run preceded F17.3. PostgreSQL-backed F17.3 tests exercise
old-cookie logout, multiple rotations, concurrent logout/refresh, limited-pool
failed logins and combined-role rejection. Additional event and filter coverage
remains required; see the current execution report for final test results.
