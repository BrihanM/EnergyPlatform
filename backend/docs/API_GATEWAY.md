# API Gateway

## Responsabilidad

`api-gateway` es el punto de entrada HTTP externo. Centraliza routing,
trazabilidad, auditoría técnica, CORS, headers de seguridad y validación de
access tokens JWT, pero no contiene reglas de negocio.

Está construido sobre Spring Cloud Gateway WebFlux y no debe incorporar
dependencias MVC.

## Organización

- `config/`: configuración de rutas, CORS, errores y headers.
- `correlation/`: validación y generación de `X-Correlation-ID`.
- `filter/`: filtros globales reactivos.
- `security/`: validación JWT, autenticación WebFlux y propagación de identidad.
- `audit/`: evento y puerto de publicación de auditoría.
- `error/`: respuesta y manejo uniforme de errores del Gateway.

## Rutas

La ruta inicial se define en `src/main/resources/application.yml`:

```yaml
- id: auth-service
  uri: ${AUTH_SERVICE_URI:http://localhost:8081}
  predicates:
    - Path=/api/v1/auth/**
```

Para registrar otro microservicio, añadir otra entrada con un `id` único, su
URI configurable por variable de entorno y sus predicates. No se debe añadir
routing al código Java ni codificar IPs de contenedores.

## Seguridad JWT

El Gateway usa Spring Security WebFlux y valida antes del routing:

- firma HMAC-SHA256;
- `iss` contra `JWT_ISSUER`;
- `exp` mediante el parser JWT;
- `sub` como UUID del usuario;
- `sid` como UUID de sesión persistida;
- `username` no vacío.

**F15.1 discrepancy and correction:** F12/F13 define the available-offer list
and detail as public, but the earlier gateway required a JWT for every Offers
route. The gateway now permits only GET list and canonical UUID detail,
including the documented `/api/v1/market/offers` alias; `/me`, mutations and
transaction routes remain protected.

El secreto se obtiene obligatoriamente de `JWT_SECRET` y debe ser externo al
repositorio, tener al menos 32 bytes y coincidir con el secreto usado por
`auth-service`. Un JWT
ausente, malformado, expirado, con firma inválida, issuer inválido o claims
inválidos produce `401 Unauthorized` sin detalles internos.
Las denegaciones futuras de autorización se responderán como `403 Forbidden`
sin exponer detalles de políticas.

F16 adds a JWT-independent route only for `POST /api/v1/market/devices/{uuid}/readings`.
The Market service authenticates the device credential; administrative device
routes remain JWT-protected and enforce ADMIN in Market.

Además de esas consultas de ofertas y la recepción técnica descrita, son públicas las operaciones `POST /api/v1/auth/register`,
`POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`,
`POST /api/v1/auth/logout`, `OPTIONS` y `GET /actuator/health` (incluyendo
subrutas de health). El resto requiere `Authorization: Bearer <JWT>`.

Tras validar el JWT, el Gateway elimina siempre los headers de identidad
recibidos del cliente y consulta a Auth antes de añadir:

```text
X-Authenticated-User-Id
X-Authenticated-Username
X-Authenticated-Roles
```

El JWT se envía al endpoint interno de Auth para su validación y permanece en
la solicitud dirigida al servicio de destino; no debe registrarse.
`X-Authenticated-Account-Id` no se
propaga porque el JWT actual no contiene `accountId`. Estos headers sólo son
confiables dentro de la red interna; los microservicios no deben exponerse
directamente a Internet.

## Variables de entorno

| Variable | Predeterminado | Propósito |
|---|---|---|
| `SERVER_PORT` | `8080` | Puerto externo |
| `AUTH_SERVICE_URI` | `http://localhost:8081` | Destino de Auth |
| `AUTH_INTROSPECTION_SECRET` | Obligatorio | Credencial interna compartida con Auth |
| `AUTH_INTROSPECTION_TIMEOUT` | `2s` | Tiempo máximo para validar en Auth |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes autorizados |
| `CORS_ALLOWED_METHODS` | `GET,POST,PUT,PATCH,DELETE,OPTIONS` | Métodos CORS |
| `CORS_ALLOWED_HEADERS` | `Authorization,Content-Type,X-Correlation-ID,Idempotency-Key` | Headers permitidos |
| `CORS_ALLOW_CREDENTIALS` | `true` | Credenciales CORS |
| `CORS_MAX_AGE` | `3600` | Duración del preflight |

En Docker, `AUTH_SERVICE_URI` se configura como `http://auth-service:8081`;
Auth Service no publica un puerto al host. El Gateway expone únicamente
`127.0.0.1:8080` por defecto y reporta su estado en `/actuator/health`.

La ruta protegida `GET /api/v1/integration/probe` es un recurso técnico de
validación. Confirma que el Gateway valida el JWT, elimina headers de identidad
enviados por el cliente y propaga la identidad confiable junto con
`X-Correlation-ID`.

## Correlation ID

El Gateway conserva un `X-Correlation-ID` válido, o genera un UUID nuevo. Sólo
acepta valores de 8 a 128 caracteres seguros (`A-Z`, `a-z`, dígitos, `.`, `_`,
`-`). El valor se propaga al upstream y se devuelve al cliente, incluidos los
errores gestionados por el Gateway.

El valor se mantiene en el `ServerWebExchange`; no se usa `ThreadLocal`.

## Auditoría

Al finalizar cada solicitud se emite un evento JSON al logger
`com.miapp.gateway.audit`. El evento contiene `eventId`, timestamps,
`correlationId`, método, path, destino, IP de origen, User-Agent, status,
duración, resultado y error técnico no sensible.

Los cuerpos, query parameters, Authorization, cookies, JWT, API keys,
contraseñas y credenciales nunca se registran. `AuditEventPublisher` permite
añadir posteriormente persistencia sin modificar el filtro.

Ejemplo:

```json
{"eventId":"...","receivedAt":"2026-10-30T10:00:00Z","completedAt":"2026-10-30T10:00:00Z","correlationId":"request-123_abc","method":"GET","path":"/api/v1/auth/status","destination":"localhost","sourceIp":"127.0.0.1","userAgent":"Mozilla/5.0","status":200,"durationMs":12,"result":"SUCCESS","error":null}
```

## Errores

Los fallos de comunicación y timeout se traducen, respectivamente, a 502 y
504 con un JSON que contiene `timestamp`, `status`, `error`, `message`, `path`
y `correlationId`. No se exponen stack traces ni detalles de infraestructura.
Los status HTTP que ya produjo un upstream no se reemplazan por este handler.

## CORS y cookies

CORS está restringido al origen configurado y no usa `*`. Se permite el header
`Authorization`, preflight `OPTIONS` y credenciales según configuración. Se añaden
`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` y
`Permissions-Policy`. El Gateway no elimina ni reescribe `Set-Cookie`, por lo
que las cookies HttpOnly de login, refresh y logout atraviesan el proxy.

Roles, permisos, RBAC y autenticación service-to-service quedan pendientes.

## Ejecución y pruebas

```powershell
cd backend
Copy-Item .env.example .env # sólo si se requiere configuración local
.\mvnw.cmd -pl services\api-gateway spring-boot:run
.\mvnw.cmd -pl services\api-gateway test
```

Las rutas públicas no requieren que Auth esté disponible. La validación de
solicitudes protegidas sí depende de Auth.
# RBAC identity propagation

For each protected request, Gateway validates the JWT locally and makes an
authenticated, uncached call directly to Auth. It replaces client-supplied
identity, role, account and internal-secret headers with the current identity
and roles returned by Auth. Invalid JWTs, inactive accounts and revoked or
expired sessions receive 401; downstream authorization failures receive 403.
An unavailable, timed-out or malformed Auth response receives 503 without
falling back to JWT roles. The call adds latency and makes Auth a dependency
for protected traffic. The internal endpoint is not exposed as a Gateway route.
Downstream services must enforce role and resource ownership and must not be
directly accessible to untrusted clients. Public offer reads and device-credential
reading ingestion do not call Auth.
