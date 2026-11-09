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

There are no event mutation or deletion APIs. Compose provisions separate Flyway
owners and runtime roles; each runtime has `SELECT` and `INSERT`, but not
`UPDATE`, `DELETE`, `TRUNCATE`, `ALTER` or `DROP`, on its own audit table.
Cross-service audit access is denied. This is not tamper-proof: database
administrators and migration owners remain trusted and can alter records.
Retention, backups and transport protection remain operational requirements.
The Flyway credentials are present in each application process; isolating
migrations in a separate deployment job would provide a stronger boundary.

## Database deployment and ownership

Compose requires distinct `AUTH_DB_USERNAME`, `AUTH_FLYWAY_USERNAME`,
`ENERGY_MARKET_DB_USERNAME` and `ENERGY_MARKET_FLYWAY_USERNAME`, with their
corresponding `*_PASSWORD` values supplied outside the repository. See
`../.env.example`. `postgres-bootstrap` checks that the four role names
are distinct, unprivileged and have no group memberships; it creates them and
grants schema access. Auth and Market then run their own historical migrations
followed by their repeatable `db/pg-grants` scripts as their respective Flyway
owners. The scripts explicitly grant operational privileges and only read and
append privileges on audit. Global and schema-specific default privileges
exclude the runtime roles and `PUBLIC`; future tables and sequences require
explicit grants. The scripts also revoke existing `PUBLIC` and cross-service
grants on the service-owned tables, so operators must review any other
consumers before rollout. The Market owner receives the `REFERENCES (id)` grant it
needs for the existing foreign keys to `public.users`.
When changing role names, operators must separately disable and revoke the
former roles after checking their dependencies; bootstrap does not discover or
remove obsolete identities.

The bootstrap refuses existing Auth tables or Market objects owned by a different
role; it does not transfer ownership or alter historical migrations. Before
upgrading an existing installation, take and test a backup, schedule a
maintenance window, inventory object ownership and effective grants, and have
an authorized database operator explicitly transfer the affected objects to
the corresponding owner after reviewing dependencies and other consumers.
Re-run bootstrap and validate the grants before restoring traffic. The checked
existing `miapp` installation has Auth V1-V4 owned by `postgres`, so it cannot
be automatically upgraded by this Compose configuration. The current Market
migrations and grants explicitly use `energy_market`; Compose rejects a
different `ENERGY_MARKET_DB_SCHEMA`.

Outside Compose, Flyway still defaults to the DataSource credentials and the
grant migrations are not enabled by default. Configure separate credentials,
`AUTH_FLYWAY_LOCATIONS` and `ENERGY_MARKET_FLYWAY_LOCATIONS` to include both
`classpath:db/migration` and `classpath:db/pg-grants`, and provision and verify
roles and ownership independently. Do not assume the runtime restrictions apply
to an unconfigured environment.

## Audit coverage matrix

The cited integration tests use PostgreSQL. Field and filter coverage is not
yet exhaustive; a listed event is not a claim that every required edge case has
been verified.

| Events | Actor / resource / result | Evidence |
|---|---|---|
| `LOGIN_SUCCEEDED`, `SESSION_REFRESHED`, `LOGOUT` | USER / SESSION / SUCCESS | `OnlineAuthorizationIntegrationTest.auditPersistsAuthenticationRolesAndAccountLifecycleWithoutCredentials` |
| `LOGIN_FAILED` | ANONYMOUS / ACCOUNT / REJECTED | Same test; concurrent pool test in `FailedLoginPoolIntegrationTest` |
| `ROLE_ASSIGNED`, `ROLE_REVOKED` | ADMIN user / USER and affected role / SUCCESS | `auditPersistsAuthenticationRolesAndAccountLifecycleWithoutCredentials`; initial bootstrap assignment uses SYSTEM |
| `USER_SUSPENDED`, `USER_REACTIVATED` | ADMIN user / USER / SUCCESS | Same test; rollback and audit-write failure tests in `OnlineAuthorizationIntegrationTest` |
| `OFFER_CREATED`, `OFFER_UPDATED`, `OFFER_CANCELLED` | USER / OFFER / SUCCESS | `EnergyTransactionIntegrationTest.offerLifecyclePersistsAuditableFacts` |
| `PURCHASE_STARTED`, `PURCHASE_CONFIRMED` | buyer / TRANSACTION / SUCCESS | `controllerExecutesAutomaticPurchaseAndReturnsVerificationSnapshot`; replay and rollback tests |
| `PURCHASE_REJECTED` | buyer / TRANSACTION / REJECTED | `rejectedPurchaseRecordsItsActualOutcome` |
| `METER_PROVISIONED`, `METER_CREDENTIAL_ROTATED`, `METER_SUSPENDED`, `METER_ACTIVATED`, `METER_REVOKED` | ADMIN user / DEVICE / SUCCESS | `MeterDeviceReadingIntegrationTest.rotatesSuspendsRevokesAndEnforcesAdmin` |
| `READING_INCONSISTENT` | DEVICE / READING / SUCCESS | `authenticatesPersistsClassifiesAndPreventsDuplicates` |

Both tables contain allow-listed scalar fields, not free-form metadata.
Successful Auth tests check user, resource, result, timestamp and absence of
the tested password and tokens; Market meter tests check absence of the tested
device credentials. Full timestamp, sensitive-field and rollback assertions for
every Market event, including rejected purchases, remain pending.

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

### Inventory and reconciliation of incompatible roles

The inspected existing Compose Auth connection targets database `miapp`,
schema `public`. That database's Auth Flyway history ends at V4; `user_roles`
is introduced by V5 and is absent. This is an older installation without the
RBAC schema, not evidence that the actual deployment has no incompatible
assignments. Confirm the deployment's JDBC URL, database, schema and migration
history before drawing conclusions. A separate disposable database `f174test`
had `public.user_roles` and zero combined assignments; it is not the existing
deployment and cannot establish its inventory.

In the correct RBAC-enabled database, use a read-only authorized connection:

```sql
SELECT current_database(), current_schema(), to_regclass('public.user_roles');
SELECT user_id, array_agg(role_code ORDER BY role_code) AS roles
FROM public.user_roles
WHERE role_code IN ('ADMIN', 'SECURITY_AUDITOR')
GROUP BY user_id
HAVING count(DISTINCT role_code) = 2;
```

Treat the output as an operational inventory of internal identifiers, not an
instruction to alter assignments. For each match, an authorized owner must
decide and record which duty should remain, review last-administrator and
account-recovery constraints, and perform an approved, auditable role change.
Verify the resulting assignments and online authorization afterwards. Never
automatically choose or remove a role; authorization continues to reject the
combined assignment until it is reconciled.

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

`mvn verify` passed for Gateway (43 tests), Auth (59) and Market (33), with
PostgreSQL-backed tests and Flyway migrations, including concurrent
reactivation/login and competing suspension cases. An isolated
F17.3 Compose stack exercised real Gateway, Auth, Market and PostgreSQL:
public offers, forged roles, two refresh rotations, old-token replay rejection,
old-cookie logout and rejection of both old and rotated JWTs. With a pool of
two connections, twelve simultaneous failed logins all returned 401 and wrote
their audit events. Additional event and filter coverage remains required.

F17.4 `mvn -f backend/pom.xml verify` passed: Gateway 43, Auth 63 and Market
33 tests, with no failures or skips. It also exercises real PostgreSQL roles in
`PostgresRuntimePrivilegesIntegrationTest` and an isolated Compose deployment.
The deployment's runtime credentials, not the migration-owner credentials,
were used for SQL `SELECT`/`INSERT` and negative `UPDATE`, `DELETE`, `TRUNCATE`,
`ALTER`, `DROP`, cross-audit read and `SET ROLE` checks. Gateway registration,
login, public offers, offer creation, confirmed purchase, history, detail,
meter provisioning, inconsistent reading, rotation and lifecycle, auditor
query RBAC, refresh and logout were exercised against the same deployment.
SQL mutation checks used a rolled-back test insertion; no audit history was
deleted. The first Market insert probe omitted required fields and failed;
the corrected probe passed.
