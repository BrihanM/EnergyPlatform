# Meter Devices and Energy Readings

## Scope and trust boundary

F16 implements simulated-device identity and immutable reading ingestion in
Energy Market. It does not connect to hardware, verify physical delivery,
create offers, or change transactions. A meter is not a user or RBAC role.
User JWTs authorize administrative operations; a separate per-device credential
authenticates ingestion. Energy Market trusts Gateway identity headers only on
the internal network. Do not expose the Market service directly.

## Provisioning and lifecycle

An ADMIN calls `POST /api/v1/market/devices` with `ownerUserId` (an existing
Auth user UUID) and a unique `deviceIdentifier` (1–128 ASCII letters, digits,
`.`, `_`, `:`, or `-`). The response contains the new `device` and its
`credential`, and has `Cache-Control: no-store`. The administrator must deliver
the credential to the intended device over a separately secured channel. It
cannot be recovered from the service.

`POST /api/v1/market/devices/{id}/credential-rotation` replaces the credential
and returns the replacement once, also with `no-store`. Rotation is allowed
for active or suspended devices; the prior credential stops working on commit.
`PATCH /api/v1/market/devices/{id}/status` accepts `{"status":"ACTIVE"}`,
`SUSPENDED`, or `REVOKED`. Revocation is terminal: a revoked device cannot be
reactivated or rotated. Status responses never contain credentials. These
endpoints require ADMIN and reject users with only NORMAL_USER or
SECURITY_AUDITOR. There is no public self-provisioning.

Credentials are 32 random bytes encoded as unpadded base64url. PostgreSQL stores
only a SHA-256 digest; verification compares digests in constant time. This
hashing scheme relies on high-entropy generated credentials, not user passwords.
Lost credentials require an administrator to rotate them. Do not log, share,
or put credentials in URLs. TLS at the external perimeter and a protected
provisioning channel are deployment requirements; the local Compose setup is
not itself a production TLS configuration.

## Ingestion contract

`POST /api/v1/market/devices/{id}/readings` does not require a user JWT.
It requires the `X-Device-Credential` header belonging to the UUID in the path.
Gateway forwards only this exact POST without JWT authentication and removes
client-supplied user-identity headers. Energy Market verifies the credential and
active status under a database row lock. Missing, invalid, unknown, suspended,
and revoked credentials receive the same generic 401. A user JWT alone does not
authenticate a device. Native clients do not use CORS; the default browser CORS
allow-list does not include `X-Device-Credential`.

The JSON body has exactly:

```json
{
  "id": "b9e21724-6fd5-49c7-8cb7-7c11622de24a",
  "generatedEnergyKwh": 2.5,
  "surplusEnergyKwh": 1.25,
  "measuredAt": "2026-01-01T10:00:00Z"
}
```

`id` is a device-generated UUID and is globally unique. A repeated UUID returns
409 and does not insert another row; a distinct UUID may use the same
measurement timestamp. This is duplicate rejection, not an idempotent replay
API. The response is 201 with the persisted reading and server-generated
`receivedAt`. The device association comes exclusively from the authenticated
path, not from the body; unknown body fields are rejected.

Values must be non-negative and fit `NUMERIC(18,6)` without rounding. The
numeric limits are representation limits, not physical plausibility thresholds.
Malformed or missing data, unsupported precision, and measurements after
receipt are rejected with 400. No undocumented clock-skew allowance is assumed.
When surplus is greater than generation, the non-negative reading is stored as
`INCONSISTENT`; otherwise it is `VALID`. The schema also reserves the documented
`PENDING` and `REJECTED` states, but this synchronous implementation does not
persist those states. Authentication failures are not persisted as readings.

## Persistence and limitations

Flyway V4 creates `energy_market.meter_devices` and `energy_market.energy_readings`,
with Auth-owner and meter foreign keys, status/amount checks, globally unique
reading IDs, a unique device identifier, and owner/measurement indexes. Device
authentication, status changes, credential rotation and reading insertion use
PostgreSQL transactions and row locks so that lifecycle changes serialize with
ingestion. Redis is not a consistency authority.

No reading is currently consumed by offers or transactions. A future consumer
must explicitly require `VALID` status and define its own authorization and
freshness policy. No physical plausibility thresholds, device rate limiting,
durable security audit, credential-delivery workflow, device listing, or
cross-device recovery protocol are implemented. Define and approve those
policies before production use.

The backend tests use PostgreSQL Testcontainers and exercise HTTP, technical
authentication, application logic and persistence. Gateway tests cover the
JWT-independent ingestion route and protected administration.
