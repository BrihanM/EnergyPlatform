# Frontend Infrastructure

This frontend is prepared to communicate with the API Gateway through a shared HTTP client and centralized environment configuration.

## Configuration

Create a local `.env` file from `.env.example` and set the API Gateway URL:

```bash
cp .env.example .env
```

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=Mi App Frontend
```

## Request flow

```text
React
  ↓
HTTP Client
  ↓
API Gateway
  ↓
Internal microservices
```

The frontend does not access internal microservices directly. All calls are routed through the API Gateway.

## Cookies and tokens

- Refresh tokens are handled by HttpOnly cookies managed by the browser.
- The frontend does not read or persist refresh tokens from JavaScript.
- Access tokens are kept only in memory through the auth provider and are never written to LocalStorage or SessionStorage.
- The HTTP client can attach the current in-memory token dynamically as a bearer header when a session exists.

## Auth state

The app exposes a global auth state through `AuthProvider` and `useAuth`:

- `initializing`
- `authenticated`
- `unauthenticated`

The provider stores the session data in memory, restores sessions on startup using `/api/v1/auth/refresh`, and keeps `initializing` until the first restore attempt finishes.

## Routing and access control

The app uses `react-router-dom` with:

- public routes: `/login`, `/register`
- protected route placeholder: `/app`
- route guards that wait while auth is `initializing`
- redirect from protected pages to `/login` for unauthenticated users
- redirect from public auth pages to the protected area for authenticated users

## HTTP client

`src/core/http/httpClient.ts` provides central HTTP utilities for:

- GET, POST, PUT, PATCH, DELETE
- headers
- query parameters
- JSON parsing
- error normalization with status, code, message, and correlationId
- credential-enabled requests for cookie-based flows
- optional authenticated request mode (`requiresAuth`) with one-time retry after automatic refresh
- single-flight refresh coordination for concurrent `401` responses on protected requests

## Not implemented yet

The following are intentionally deferred and should be added in dedicated feature modules later:

- RBAC and permissions
- feature-complete dashboard
- forgot-password / password-reset / MFA
