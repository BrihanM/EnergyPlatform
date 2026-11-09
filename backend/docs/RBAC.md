# Role-Based Access Control

## Roles

The closed catalogue contains only `NORMAL_USER`, `ADMIN`, and
`SECURITY_AUDITOR`. Normal users publish, browse, edit/cancel their own offers,
buy, and view their own resources. Administrators manage users and role
assignments. Security auditors have read-only access to audit and compliance
evidence; they cannot manage users, roles, offers, or transactions.

## Persistence

Flyway migration `V5__create_roles.sql` creates `roles` and `user_roles`, seeds
the three official role codes, and enforces PK, FK, unique-assignment, and
catalogue checks. New registration assigns `NORMAL_USER` within the same
transaction. Historical migrations are unchanged.

## JWT and Gateway contract

Auth emits an array claim named `roles`, loaded from current persistence on login
and refresh. Gateway validates the claim against the closed catalogue and rejects
missing, empty, or unknown values. It removes client-provided
`X-Authenticated-User-Id`, `X-Authenticated-Username`, and
`X-Authenticated-Roles`, then reconstructs them from the validated JWT.

Downstream services must enforce both role and ownership; propagated headers are
not an authorization substitute and are trusted only on the private service
network.

## Administration

`GET /api/v1/admin/roles` lists the catalogue.
`GET /api/v1/admin/users/{userId}/roles` reads assignments.
`POST /api/v1/admin/users/{userId}/roles` with `{"role":"ADMIN"}` assigns a role.
`DELETE /api/v1/admin/users/{userId}/roles/{role}` revokes a role.

All endpoints require `ADMIN`, use DTOs/enums rather than JPA entities, and
return `403` for authenticated non-admin callers. Changes are logged with actor,
target, action, and result. The last active administrator cannot be revoked.

## Bootstrap and operations

Set `RBAC_BOOTSTRAP_ADMIN_USERNAME` to an existing active account before the
first startup. Auth assigns `ADMIN` only if no active administrator exists. It
does not accept a password, create an account, log credentials, or overwrite an
existing administrator. If the account is absent/inactive, startup continues
without granting a role and emits a warning.
