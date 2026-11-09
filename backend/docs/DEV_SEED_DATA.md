# Development seed data (F19)

Creates demo accounts, meters, readings, offers, transactions and audit events for the
EnergyProsumer testbed. It is **for development and demonstrations only**. It must not run in production.

## What is created

| Item | Default | Variable |
|---|---|---|
| ADMIN | 1 (`demo-admin`) | `SEED_DEMO_ADMIN_USERNAME` |
| SECURITY_AUDITOR | 1 (`demo-auditor`) | `SEED_AUDITOR_USERNAME` |
| NORMAL_USER | 8 (`demo-user-01` … `demo-user-08`) | `SEED_NORMAL_USERS` |
| Meters | 4 (`demo-meter-01` … `demo-meter-04`) | `SEED_DEVICES` |
| Energy readings | 10 per meter (40 total) | `SEED_READINGS_PER_DEVICE` |
| Offers | 20 total, 2 cancelled, the rest ACTIVE | `SEED_OFFERS_TOTAL`, `SEED_CANCELLED_OFFERS` |
| Transactions | 10 confirmed | `SEED_TRANSACTIONS` |

Every record goes through the public API (`/api/v1/...` via the gateway), so the existing
validations, roles and audit events apply. The only exception is the first ADMIN: the HTTP API cannot
create it, so auth-service creates it in-process when `DEV_SEED_ENABLED=true` (`DevSeedAdminBootstrap`).

## Requirements and limitations

- Rejected transactions are **not** generated. The transaction simulator has one global mode per
  run (`SUCCESS`, `REJECT`, `RULE_BASED`), so confirmed and rejected results cannot be mixed in one seed.
- EXPIRED offers are not generated because `expiresAt` must be in the future through the API.
- The ADMIN is created only if no active administrator exists. If another admin already exists,
  `demo-admin` is not created and the seed fails at its first login with a generic message.
- Re-runs are idempotent for users, roles, offers and transactions. Existing devices are skipped
  (there is no endpoint to look up a device again), so their readings are not re-sent.
- If a run fails midway, reset the environment (below) instead of patching the data.

## Configuration

Set these in `backend/.env` (see `.env.example`). Do not commit real values.

```
DEV_SEED_ENABLED=true               # enables the in-process ADMIN bootstrap in auth-service
SEED_DEMO_PASSWORD=<min 12 chars, letters and digits>
```

`SEED_DEMO_PASSWORD` is the single password used for all demo accounts. It is never printed, logged
or stored in code. Leave `DEV_SEED_ENABLED=false` (the default) in any other environment.

## Run

From `backend/`, with the stack built:

```
docker compose --profile seed up --build -d
docker compose --profile seed logs -f demo-seed
```

The `demo-seed` service starts only after the gateway, auth-service and energy-market-service report
healthy, and exits with `restart: no`. Its last log line is `[seed] done: ...` with the counts.

Run it again to confirm idempotency: the counts for created records must be 0.

## Demo credentials

| Account | Username | Password | Roles |
|---|---|---|---|
| Administrator | `demo-admin` | `SEED_DEMO_PASSWORD` | ADMIN |
| Auditor | `demo-auditor` | `SEED_DEMO_PASSWORD` | SECURITY_AUDITOR |
| Users | `demo-user-01` … `demo-user-08` | `SEED_DEMO_PASSWORD` | NORMAL_USER |

Log in with `POST /api/v1/auth/login` and body `{"identifier": "<username>", "password": "<password>"}`.

## Reset the test environment

```
docker compose --profile seed down -v
docker compose --profile seed up --build -d
```

`down -v` removes the Postgres volume, so it deletes **all** data, not only the demo data. Use it only
in development.

## Security notes

- Passwords are hashed by auth-service; device credentials are returned once by the API and kept in
  memory only for the current run.
- The script prints only progress counts and HTTP status codes. It never prints tokens, passwords or
  credentials.
- Quantities can be changed with the `SEED_*` variables; invalid combinations fail before any request.

## Tests

```
cd backend/infrastructure/seed
python -m unittest test_seed_demo_data
```

The tests use an in-memory fake of the API and cover configuration validation, quantities, idempotent
re-runs, buyer/seller relationships, and absence of secrets in output.
