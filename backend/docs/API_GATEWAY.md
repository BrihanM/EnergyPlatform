# API Gateway

## Responsabilidad

`api-gateway` es el punto de entrada HTTP externo. Centraliza routing,
trazabilidad, auditoría técnica, CORS y headers de seguridad, pero no contiene
reglas de negocio ni autentica usuarios en esta fase.

Está construido sobre Spring Cloud Gateway WebFlux y no debe incorporar
dependencias MVC.

## Organización

- `config/`: configuración de rutas, CORS, errores y headers.
- `correlation/`: validación y generación de `X-Correlation-ID`.
- `filter/`: filtros globales reactivos.
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

## Variables de entorno

| Variable | Predeterminado | Propósito |
|---|---|---|
| `SERVER_PORT` | `8080` | Puerto externo |
| `AUTH_SERVICE_URI` | `http://localhost:8081` | Destino de Auth |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes autorizados |
| `CORS_ALLOWED_METHODS` | `GET,POST,PUT,PATCH,DELETE,OPTIONS` | Métodos CORS |
| `CORS_ALLOWED_HEADERS` | `Content-Type,X-Correlation-ID` | Headers permitidos |
| `CORS_ALLOW_CREDENTIALS` | `false` | Credenciales CORS |
| `CORS_MAX_AGE` | `3600` | Duración del preflight |

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

## CORS y seguridad

CORS está restringido al origen configurado y no usa `*`. Se añaden
`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` y
`Permissions-Policy`. JWT, login, refresh tokens, roles y autenticación
service-to-service quedan pendientes.

## Ejecución y pruebas

```powershell
cd backend
Copy-Item .env.example .env # sólo si se requiere configuración local
.\mvnw.cmd -pl services\api-gateway spring-boot:run
.\mvnw.cmd -pl services\api-gateway test
```

La ruta no requiere que Auth Service tenga endpoints implementados para
arrancar; las solicitudes serán auditadas aunque el destino no esté disponible.
