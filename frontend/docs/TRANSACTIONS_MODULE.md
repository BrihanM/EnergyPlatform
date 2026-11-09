# Frontend transactions module

## Scope and architecture

F15 adds simulated purchases, own transaction history, and transaction detail.
`src/features/transactions` contains the API contracts and service, the purchase
hook and pending-intention storage, reusable transaction display, and pages.
Offer detail provides the purchase entry point. `App.tsx` provides History API
routes `/transactions` and `/transactions/:id`.

The existing HTTP client supplies authentication and refresh handling. The UI
requires an authenticated `NORMAL_USER` with a known user ID and excludes
`ADMIN` and `SECURITY_AUDITOR` accounts. The backend remains responsible for
authorization, ownership, price, quantity and availability.

## Backend contract

- `POST /api/v1/transactions`: `{ "offerId": "<uuid>" }` and
  `Idempotency-Key`. The response contains the transaction snapshot.
- `GET /api/v1/transactions/me?size=20&offset=0`: an array of the current
  user's transactions as buyer or seller, ordered by the backend. No total or
  participant filter is returned. A full page enables Next; consequently the
  next page can be empty.
- `GET /api/v1/transactions/{id}`: participant-only detail.

The response contains IDs, numeric energy/price/total, currency, status,
creation/completion times and nullable verification fields. The service validates
the expected shape before presenting it. The frontend distinguishes
`CONFIRMED`, `REJECTED`, `PENDING_VERIFICATION` and `CANCELLED`. Historical
documentation that refers to `/transactions/history` or
`/api/v1/market/transactions` does not match the implemented controller.

## Purchase and idempotency

The user sees the offer and a confirmation explaining that no payment or real
energy transfer takes place. One click submits a purchase; the simulator and
verification are automatic. The UI prevents buying one's own offer, a
non-active offer or an expired offer. The backend checks these rules again.

Before sending, the hook generates a UUID and persists the offer ID and key
under the authenticated user ID in localStorage. No credentials or keys are
logged or shown. The same key is reused after errors, refreshes and retries;
another offer is blocked until the pending intention is resolved. A validated
terminal response clears the intention; `PENDING_VERIFICATION` retains it and
records the transaction ID. An in-page lock and the Web Locks API prevent
duplicate submissions across tabs in the same browser profile. A browser
without Web Locks cannot submit a purchase and displays an explanation.

An HTTP or communication error is **not** presented as a simulator rejection or
proof that no purchase occurred. History provides a recovery action that
retries the same key even when the offer detail is no longer available. A
malformed response also leaves the intention intact. Browser storage failures
stop new requests and are shown as errors. Purchase requests time out after
30 seconds; a timeout also leaves the intention intact.
Retry controls require the same pending intention to still exist under the
cross-tab lock. A stale tab cannot create a new key after another tab has
resolved or replaced that intention.

## Operational and security limits

- An existing `CORS_ALLOWED_HEADERS` override must include `Idempotency-Key`.
  The gateway, Compose default and example environment now include it.
- Clearing browser storage or using another browser/profile can lose the
  locally stored intention. Do not retry an uncertain purchase from another
  browser or clear storage before reconciling its history.
- The backend does not expose cross-device reconciliation. History may show a
  completed transaction, but its absence does not prove that an uncertain
  operation did not happen. A permanently unresolved intention cannot be
  discarded through the UI; consult the backend before clearing storage.
  A separate backend enhancement could provide authenticated reconciliation
  through an opaque operation identifier and an explicit retention policy,
  without exposing idempotency keys or changing the existing purchase contract.
- Backend decimal values are JSON numbers, so JavaScript numeric precision
  limits apply. Offer values are converted to strings for the existing UI.
- Detail failures including 403, 404 and invalid responses show the same safe
  message. Verification reasons are not rendered as arbitrary server text.

## Validation

F15.1 exercised an isolated Docker Compose stack with real Auth, Gateway,
Market, PostgreSQL and Redis. The API checks covered registration, login and
role refresh, anonymous offer queries, CORS preflight, forged identity headers,
exclusive ADMIN and SECURITY_AUDITOR accounts, purchase, replay, participant
history and detail, and denial of a foreign transaction. Test-only role setup
was performed in the isolated database.

A real browser test also simulated losing a committed purchase response,
reloaded the page and replayed the same intention from another tab. It verified
the same transaction and a single history entry, then confirmed that a stale
third tab could not send a new purchase after resolution.

The Vitest suite exercises purchase outcomes, validation/permission errors,
same-key retry, stale-tab protection, duplicate submission, pending status,
history pagination and roles, detail errors, incomplete responses, API headers,
and direct routes.
Run `npm test`, `npm run lint`, and `npm run build` in `frontend`. The gateway
security tests and all backend tests can be run with `mvn test` from `backend`.
