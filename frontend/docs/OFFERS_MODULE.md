# Frontend Offers Module

The feature lives under `src/features/offers` and uses the existing
`core/http/httpClient`. It does not store tokens, seller identifiers, or offer
state locally as a substitute for the API.

## Screens and routes

- `/` — public marketplace with server-side filters and pagination.
- `/offers/:id` — public detail for an available offer.
- `/offers/new` — authenticated `NORMAL_USER` publication form.
- `/offers/mine` — authenticated `NORMAL_USER` management view.
- `/login` — existing authentication page.

The application currently has no router dependency, so `App.tsx` provides the
minimal History API routing needed by these screens. Backend authorization
remains authoritative.

## API integration

The service calls `GET /api/v1/offers`, `GET /api/v1/offers/{id}`,
`GET /api/v1/offers/me`, `POST /api/v1/offers`, `PUT /api/v1/offers/{id}`, and
`DELETE /api/v1/offers/{id}`. Requests use the existing client, which attaches
the in-memory access token and includes refresh cookies.

Numeric values remain strings at the API boundary. The indicative total uses
integer (`BigInt`) decimal multiplication before COP formatting. No purchase
action is exposed.

## Permissions and UX

The login contract's roles are retained in the auth context. Publication,
editing, and cancellation controls are rendered only for `NORMAL_USER`; the
server still validates every operation. Forms validate positive values,
precision limits, and future expiration. Cancellation requires confirmation
and leaves the offer visible in its `CANCELLED` state.

Loading, empty, API error, unavailable detail, pagination, and mutation states
are represented explicitly. HU-23 is partially covered: offer marketplace and
management UI are implemented, while transactions, history, and other future
features remain pending.
