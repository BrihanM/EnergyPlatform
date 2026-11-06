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
- `username` no vacío.

El secreto se obtiene obligatoriamente de `JWT_SECRET` y debe ser externo al
repositorio, tener al menos 32 bytes y coincidir con el secreto usado por
`auth-service`. Un JWT
ausente, malformado, expirado, con firma inválida, issuer inválido o claims
inválidos produce `401 Unauthorized` sin detalles internos.
Las denegaciones futuras de autorización se responderán como `403 Forbidden`
sin exponer detalles de políticas.

Son públicas únicamente las operaciones `POST /api/v1/auth/register`,
`POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`,
`POST /api/v1/auth/logout`, `OPTIONS` y `GET /actuator/health` (incluyendo
subrutas de health). El resto requiere `Authorization: Bearer <JWT>`.

Tras validar el JWT, el Gateway elimina siempre los headers de identidad
recibidos del cliente y añade:

```text
X-Authenticated-User-Id
X-Authenticated-Username
```

El token no se reenvía ni se registra. `X-Authenticated-Account-Id` no se
propaga porque el JWT actual no contiene `accountId`. Estos headers sólo son
confiables dentro de la red interna; los microservicios no deben exponerse
directamente a Internet.

## Variables de entorno

| Variable | Predeterminado | Propósito |
|---|---|---|
| `SERVER_PORT` | `8080` | Puerto externo |
| `AUTH_SERVICE_URI` | `http://localhost:8081` | Destino de Auth |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes autorizados |
| `CORS_ALLOWED_METHODS` | `GET,POST,PUT,PATCH,DELETE,OPTIONS` | Métodos CORS |
| `CORS_ALLOWED_HEADERS` | `Authorization,Content-Type,X-Correlation-ID` | Headers permitidos |
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

La ruta no requiere que Auth Service tenga endpoints implementados para
arrancar; las solicitudes serán auditadas aunque el destino no esté disponible.
# RBAC identity propagation

After validating the JWT signature, issuer, expiry, UUID subject, username, and
closed `roles` claim, the Gateway removes client-supplied identity headers and
rebuilds `X-Authenticated-User-Id`, `X-Authenticated-Username`, and
`X-Authenticated-Roles`. Downstream services must treat these headers as an
internal transport contract only and must still enforce resource ownership.
Requests without a valid token cannot establish identity or roles.
