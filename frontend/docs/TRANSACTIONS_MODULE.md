# Transactions module: blocked

## Scope and current status

F15 is not implemented in this checkout. Purchase, transaction history and
transaction detail depend on a market service and gateway routes that are not
present in the current branch. No transaction types, services, screens,
endpoints or idempotency keys have been added to the frontend. The existing
Offers UI continues to have no purchase action.

The current gateway routes authentication and the integration probe only; its
default CORS allowed headers do not include `Idempotency-Key`. The current
Compose configuration also does not define an energy-market service. The
compiled files under the ignored `target` directory are build artifacts, not
the source or a deployable service contract for this checkout.

## Contract discrepancy

The repository contains transaction source in commit `41f07e3` on branch
`develop-b`, which is not an ancestor of this checkout. This is historical
evidence, not a contract implemented by the current branch. That revision
defines:

- `POST /api/v1/transactions`, accepting `{ offerId }` and an
  `Idempotency-Key` header.
- `GET /api/v1/transactions/me`, returning a list with `size` and `offset`
  parameters, not a Spring page response.
- `GET /api/v1/transactions/{id}`.
- `PENDING_VERIFICATION`, `CONFIRMED`, `REJECTED` and `CANCELLED` statuses.

Its controller marks the idempotency header optional, but the service rejects
null, blank, malformed or overly long keys. Its default gateway CORS headers
also omit `Idempotency-Key`, so a cross-origin browser request using that header
requires an explicit CORS configuration change. These observations must be
rechecked against the actual backend revision deployed before implementation.
In particular, the `GET /transactions/history` path in the existing story
traceability document is not the history endpoint in that historical revision.

## Required follow-up

Bring the intended market service and its gateway routing into this branch
through the project's backend workflow, including CORS support for the
idempotency header. Reconfirm authentication, ownership, error responses,
pagination, transaction snapshots and idempotent retry behavior against the
deployed code before implementing the frontend. A communication failure must
not be treated as proof that a transaction was not recorded.

The frontend also has pre-existing merge conflict markers in
`src/core/auth/types.ts` and `src/core/http/types.ts`; these prevent a production
build and need to be resolved separately. F15 does not modify Auth or the
backend.

## Story traceability

This section supplements
[the existing story matrix](../../backend/docs/USER_STORY_TRACEABILITY.md)
without changing backend files:

| Story | Status in this checkout |
| --- | --- |
| HU-20 | Pending: transaction history and its backend integration are unavailable. |
| HU-23 | Partial: the Offers frontend exists, but purchase, history and transaction detail are not implemented or validated end-to-end. |

## Validation

No transaction tests have been added because no transaction integration has
been implemented. Baseline validation on this checkout:

- `npm test`: 20 passed, 1 failed. The existing HTTP client test expected
  `Content-Type: application/json` but received no content type.
- `npm run lint`: failed on the conflict markers in the two type files.
- `npm run build`: failed with TypeScript errors on those conflict markers.

Direct navigation, route refresh, and Offers/Auth end-to-end behavior remain
unverified. These checks must be repeated after the blockers are resolved.
