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

`App.tsx` currently provides the History API routing used by these screens.
Backend authorization remains authoritative.
The gateway allows anonymous GET access to the offer list and UUID detail;
`/offers/me`, offer mutations and transactions still require authentication.

## API integration

The service calls `GET /api/v1/offers`, `GET /api/v1/offers/{id}`,
`GET /api/v1/offers/me`, `POST /api/v1/offers`, `PUT /api/v1/offers/{id}`, and
`DELETE /api/v1/offers/{id}`. Requests use the existing client, which attaches
the in-memory access token and includes refresh cookies.

The backend sends JSON numbers; the service normalizes energy and price to the
existing string contract. The indicative total uses integer (`BigInt`) decimal
multiplication before COP formatting. Available offers can be purchased from
their detail page by eligible users; see [Transactions](./TRANSACTIONS_MODULE.md).

## Permissions and UX

The login contract's roles are retained in the auth context. Publication,
editing, and cancellation controls are rendered only for `NORMAL_USER`; the
server still validates every operation. Forms validate positive values,
precision limits, and future expiration. Cancellation requires confirmation
and leaves the offer visible in its `CANCELLED` state.

Loading, empty, API error, unavailable detail, pagination, and mutation states
are represented explicitly. F15.1 validated anonymous list/detail through the
real gateway and market service, while private routes and role restrictions
remained protected. Authenticated browser validation covered a simulated
purchase, recovery after response loss, history and detail. HU-23 remains
partially covered: the marketplace, management and transaction UI are
implemented, while other future features remain pending.
