# Auth Service

## Responsabilidad y arquitectura

`auth-service` es responsable de la identidad de usuario y de las cuentas
utilizadas para acceder a la plataforma. Está organizado con arquitectura
hexagonal: el caso de uso vive en `application`, las reglas y modelos puros en
`domain`, y los adaptadores REST, JPA y de hashing en `infrastructure`.

## Registro de usuario

El caso de uso `RegisterUserUseCase` recibe un `RegisterUserCommand`, valida las
reglas de negocio, normaliza los identificadores, comprueba duplicados, genera
el hash de contraseña, crea `User` y `Account`, y persiste ambos dentro de una
transacción. Las restricciones de PostgreSQL son la última barrera ante carreras
entre solicitudes concurrentes; sus violaciones se traducen a errores seguros.

`User` contiene `firstName`, `lastName`, `createdAt` y `updatedAt`. `Account`
contiene la relación por `userId`, `username`, `email`, `passwordHash`, estado y
fechas de auditoría. La contraseña original nunca se almacena ni se devuelve.

## Normalización y unicidad

`username` y `email` se recortan y convierten a minúsculas con `Locale.ROOT`
antes de consultar o persistir. El nombre de usuario acepta entre 3 y 50
caracteres alfanuméricos, puntos, guiones y guiones bajos, sin separadores al
inicio o al final. El correo usa validación HTTP de formato y una comprobación
de negocio equivalente.

La migración `V2__register_user_account.sql` crea restricciones que sólo
permiten valores ya normalizados y los índices únicos correspondientes. Así la
base de datos impide tanto duplicados exactos como escrituras directas en
mayúsculas que evadirían la política; también mantiene la unicidad cuando dos
solicitudes concurrentes llegan al mismo tiempo. La migración transforma las
columnas de la estructura inicial V1 sin modificarla.

## Credenciales

La política inicial exige una contraseña de 12 a 128 caracteres, con al menos
una letra y un número. El adaptador `PasswordHashingAdapter` usa
`BCryptPasswordEncoder`; el dominio y la aplicación dependen únicamente del
puerto `PasswordHashingPort`. Los comandos sobrescriben `toString()` para
redactar la contraseña.

## API

### `POST /api/v1/auth/login`

Valida un `identifier` que puede ser `username` o `email`, junto con la
contraseña. El identificador se recorta y se normaliza a minúsculas con la
misma política utilizada durante el registro.

Request:

```json
{
  "identifier": "brayan.marin",
  "password": "ContraseñaSegura1"
}
```

También se permite:

```json
{
  "identifier": "brayan@example.com",
  "password": "ContraseñaSegura1"
}
```

Una autenticación válida requiere una cuenta `ACTIVE` y responde `200 OK`:

```json
{
  "authenticated": true,
  "user": {
    "id": "uuid",
    "firstName": "Brayan",
    "lastName": "Marín",
    "username": "brayan.marin",
    "email": "brayan@example.com",
    "createdAt": "2026-10-31T00:00:00Z"
  }
}
```

Las cuentas `LOCKED`, `DISABLED`, inexistentes y las contraseñas incorrectas
producen la misma respuesta `401 Unauthorized` con el mensaje genérico
`Credenciales inválidas.`. Esto evita revelar si un username o email está
registrado. Para cuentas inexistentes se realiza además una comparación contra
un hash señuelo para reducir diferencias de tiempo observables.

La fase de login valida credenciales, pero todavía no genera JWT ni access
tokens. La infraestructura de sesiones y refresh tokens está preparada para
ser invocada por la siguiente fase, pero aún no existe un endpoint de refresh,
rotación funcional ni logout.

### `POST /api/v1/auth/register`

Request:

```json
{
  "firstName": "Brayan",
  "lastName": "Marín",
  "username": "brayan.marin",
  "email": "brayan@example.com",
  "password": "ContraseñaSegura1"
}
```

La respuesta es `201 Created`, incluye un header `Location` y devuelve sólo
datos seguros:

```json
{
  "id": "uuid",
  "firstName": "Brayan",
  "lastName": "Marín",
  "username": "brayan.marin",
  "email": "brayan@example.com",
  "status": "ACTIVE",
  "createdAt": "2026-10-31T00:00:00Z"
}
```

Los datos inválidos producen `400`, los identificadores duplicados `409`, y
los errores inesperados de persistencia se traducen a una respuesta segura.
El formato contiene `timestamp`, `status`, `error`, `message`, `path` y el
`X-Correlation-ID` recibido, cuando existe. No se incluyen SQL, stack traces,
hashes ni credenciales.

## Configuración y pruebas

La conexión se configura con `AUTH_DB_URL`, `AUTH_DB_USERNAME`,
`AUTH_DB_PASSWORD`, `POSTGRES_DB` y `POSTGRES_PORT`. Las pruebas unitarias
validan normalización, política de contraseña, duplicados y ausencia de
secretos. Las pruebas de persistencia usan PostgreSQL 18 mediante Testcontainers
cuando Docker está disponible y comprueban Flyway, relación y restricciones
únicas.

```powershell
Set-Location backend
.\mvnw.cmd -pl services/auth-service -am test
```

## Sesiones y refresh tokens

`Session` relaciona una cuenta con una sesión independiente; una cuenta puede
tener múltiples sesiones activas. La migración `V3__create_sessions.sql`
persiste `account_id`, el hash del refresh token, expiración, fechas de
auditoría y `revoked_at`. Revocar una sesión es una operación lógica y no
elimina el registro. La entidad permite reemplazar el hash y la expiración,
dejando preparada la rotación futura.

El refresh token se genera con `SecureRandom` usando 32 bytes (256 bits) y
Base64 URL-safe sin padding. Como el token tiene alta entropía, se persiste
únicamente su digest SHA-256 codificado en Base64 URL-safe; el valor real sólo
vive en memoria durante la creación. No se registra, no se incluye en JSON y
`CreateSessionResult` redacta el valor en `toString()`.

La expiración se configura con `REFRESH_TOKEN_EXPIRATION` en milisegundos
(también acepta duración ISO-8601) y se calcula usando `Clock`. La cookie
centralizada `RefreshTokenCookieManager` usa por defecto:

- `HttpOnly=true`, por lo que JavaScript no puede leerla.
- `Secure` configurable por ambiente; debe ser `true` en producción.
- `SameSite=Lax` configurable. `SameSite=None` exige `Secure=true`.
- `Path=/api/v1/auth`, limitado a login, refresh y logout futuros.
- `Domain` vacío por defecto, lo que crea una cookie host-only; se puede
  configurar explícitamente para subdominios.
- `Max-Age` configurable mediante `REFRESH_TOKEN_COOKIE_MAX_AGE`.

El gateway mantiene la cookie `Set-Cookie` al enrutar la respuesta y CORS
permite credenciales con orígenes explícitos (`CORS_ALLOWED_ORIGINS`).
Nunca se debe combinar `allowCredentials=true` con `*`. El frontend futuro
deberá enviar las solicitudes relevantes con `withCredentials: true`.

La siguiente fase implementará el flujo `Login -> Session + access token JWT +
cookie`, además de `/refresh`, rotación completa y `/logout`. JWT, refresh
endpoint, sesiones HTTP, roles y autorización siguen fuera de esta fase.
