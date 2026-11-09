# User Story Traceability

The sprint allocation below follows the eight-sprint organization supplied for
the project. F01-F08 authentication and the F10 RBAC work are recorded as implemented only
where evidenced by current code and tests. F12 offer stories are marked
implemented only for the validated backend scope; frontend and transaction work
remain pending.

| HU | Module | Entities | Future endpoint/feature | Sprint | Dependencies | Technical status |
|---|---|---|---|---:|---|---|
| HU-01 | Auth | User, Account | `POST /api/v1/auth/register` | 1 | PostgreSQL, Flyway | Implemented/validated in current Auth |
| HU-02 | Auth | Account, Session | `POST /api/v1/auth/login` | 1 | BCrypt, JWT | Implemented/validated in current Auth |
| HU-03 | Auth/Operations | Configuration | Environment validation | 1 | Docker Compose | Implemented as environment-based configuration; operational review remains |
| HU-04 | Auth/Gateway | Error responses | Generic safe error envelope | 1 | Correlation ID | Implemented in current handlers |
| HU-05 | Auth | Session | refresh/logout endpoints | 2 | HttpOnly cookie, rotation | Implemented/validated in current Auth |
| HU-06 | Auth/Gateway | Account, future Role | Role claims and route authorization | 2 | Approved RBAC model | Pending; no role model currently present |
| HU-07 | Auth/Market | Request DTOs, domain entities | Boundary and domain validation | 3 | HU-06, market service | Pending |
| HU-08 | Gateway/Infrastructure | Rate-limit keys | Gateway/API rate limiting | 3 | Redis, policy | Pending |
| HU-09 | Market/offers | EnergyOffer | `POST /api/v1/offers` | 4 | HU-06, DB design | Implemented/validated in F12 backend |
| HU-10 | Market/offers | EnergyOffer | `GET /api/v1/offers`, `GET /api/v1/offers/{id}`, `/me` | 4 | HU-09 | Implemented/validated in F12 backend |
| HU-11 | Market/offers | EnergyOffer | `PUT`/`DELETE /api/v1/offers/{id}` owner offer | 4 | HU-09, lifecycle rules | Implemented/validated in F12 backend |
<<<<<<< HEAD
| HU-12 | Market/transactions | Offer, Transaction | `POST /api/v1/transactions` | 5 | Atomic locking, HU-09 | Implemented/validated in F14 backend; frontend purchase remains out of scope |
| HU-13 | Market/verification | Transaction, Verification | synchronous verification/closure workflow | 5 | HU-12, simulator | Implemented/validated in F14 backend |
=======
| HU-12 | Market/transactions | Offer, Transaction | `POST /api/v1/market/transactions` | 5 | Atomic locking, HU-09 | Pending |
| HU-13 | Market/verification | Transaction, Verification | verification/closure workflow | 5 | HU-12, simulator | Pending |
>>>>>>> develop-f
| HU-14 | DevSecOps | Build dependencies | Dependency scan pipeline | 6 | CI policy | Pending |
| HU-15 | Market/readings | MeterDevice, EnergyReading | authenticated device reading ingestion | 6 | Device credential policy | Pending |
| HU-16 | Gateway/Operations | Network configuration | internal network and perimeter checks | 6 | Docker topology | Partially present in Compose; acceptance evidence pending |
| HU-17 | Frontend/auth | Auth session | Login/register/protected routes | 1-2 | Auth endpoints | Core Auth implementation present; broader route validation pending |
| HU-18 | Market/Infrastructure | Device credential, audit metadata | encryption at rest/in transit design | 7 | Key-management decision | Pending |
| HU-19 | Gateway/Market | AuditEvent | event persistence and emission | 7 | Audit ownership/schema | Technical Gateway events present; durable business audit pending |
<<<<<<< HEAD
| HU-20 | Market/transactions | Transaction | `GET /api/v1/transactions/me` | 7 | HU-12, authorization | Backend history implemented in F14; frontend remains pending |
| HU-21 | Audit | AuditEvent | auditor query/filter UI/API | 7 | HU-06, HU-19 | Pending |
| HU-22 | Administration | User, Account, Role | suspend/list users | 8 | HU-06, Auth ownership contract | Pending; must not bypass Auth ownership |
| HU-23 | Frontend/market | Offer, Transaction | offers and transaction pages | 8 | HU-09-HU-13 | Partially implemented: offers UI in F13; transaction UI pending by F14 scope |
=======
| HU-20 | Market/transactions | Transaction | `GET /transactions/history` | 7 | HU-12, authorization | Pending |
| HU-21 | Audit | AuditEvent | auditor query/filter UI/API | 7 | HU-06, HU-19 | Pending |
| HU-22 | Administration | User, Account, Role | suspend/list users | 8 | HU-06, Auth ownership contract | Pending; must not bypass Auth ownership |
| HU-23 | Frontend/market | Offer, Transaction | marketplace, offer management and transaction pages | 8 | HU-09-HU-13 | Partially implemented in F13: marketplace and offer management UI; transactions/history pending |
>>>>>>> develop-f
| HU-24 | DevSecOps | All | security and concurrency tests | 8 | Implemented market flows | Pending |
| HU-25 | Compliance | Traceability matrix | normative mapping and evidence | 8 | Security controls and audit | Pending |

## Current frontend baseline

`frontend/src/features/auth` contains the current authentication page and
service. `src/core/auth` holds in-memory/session token state and
`src/core/http` sends credentials and bearer tokens by default. `App.tsx`
currently renders the authentication page directly; dedicated routing and
protected/public route components are future work from the repository snapshot.

The target feature layout is:

```text
frontend/src/features/
  auth/
  offers/
  transactions/
  energy-readings/
  administration/
  audit/
```

Each feature should own `components`, `hooks`, `services`, `types`, and `pages`.
Cross-cutting HTTP, auth, configuration, and contexts stay in `src/core`;
generic controls and utilities stay in `src/shared`. Market state must be
loaded from PostgreSQL-backed APIs, never represented only by hardcoded React
data. Existing `HttpError` handling and TypeScript conventions should be reused.

## Status interpretation

“Implemented/validated” refers to current Auth source and tests, not to an
independent academic claim. No new evidence is inferred for HU-06 onward.
