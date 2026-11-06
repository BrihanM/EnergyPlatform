# Arquitectura

La plataforma es un monorepo Maven de microservicios. Cada servicio posee un
bounded context, lógica de negocio y persistencia independientes, y puede
evolucionar y desplegarse sin acoplarse al resto.

```text
Frontend
   |
   v
API Gateway
   |-- routing, CORS, Correlation ID
   |-- JWT validation and identity propagation
   v
Auth Service
   |-- User, Account, Session
   |-- register, login, refresh rotation, logout
   v
PostgreSQL
```

The initial Energy Market boundary is also present behind the Gateway:

```text
API Gateway -- /api/v1/market/** --> Energy Market Service --+
                                      energy_market schema   |
PostgreSQL <-----------------------------------------------+
```

Energy Market uses the same PostgreSQL instance but an independent
`energy_market.flyway_schema_history`; Auth retains ownership of its public
schema and history. The service is internal-only in Docker Compose and has no
business endpoints until the next phase.

En desarrollo, Docker Compose levanta PostgreSQL y Redis, mientras las
aplicaciones se ejecutan desde Maven. En producción, Gateway y Auth Service
deben conectarse a una red interna y sólo Gateway debe aceptar tráfico externo.

## Hexagonal

- `domain`: entidades, objetos de valor y reglas de negocio puras; no depende de Spring, PostgreSQL, Redis ni otras tecnologías.
- `application`: casos de uso, servicios de aplicación y puertos de entrada/salida; coordina el dominio sin conocer implementaciones concretas.
- `infrastructure`: adaptadores de entrada y salida, persistencia, integraciones y configuración técnica; implementa los puertos internos.

La dependencia apunta hacia el interior: infrastructure -> application -> domain. Los controladores y repositorios no contienen reglas de negocio.

## Despliegue Docker

El tráfico externo entra únicamente por `api-gateway` en el puerto `8080`.
Gateway está conectado a `gateway-edge` y `backend-internal`; `auth-service`,
PostgreSQL y Redis sólo están conectados a `backend-internal`, que es una red
interna de Docker. El flujo de ejecución es:

```text
Client -> API Gateway -> Auth Service -> PostgreSQL
                             |
                             -> Redis (infraestructura disponible)
```

El entorno reproducible se inicia con `docker compose up --build`. PostgreSQL
y Redis tienen health checks, y Auth Service espera a PostgreSQL antes de
ejecutar Flyway y arrancar.

## Responsabilidades transversales

El Gateway es el punto de entrada externo, encargado de routing, validación
JWT, filtros, correlation IDs, metadatos de auditoría y controles perimetrales.
No tiene reglas de negocio. La auditoría nunca registra contraseñas, tokens,
credenciales ni datos sensibles.

La comunicación futura usará contratos explícitos y mecanismos desacoplados. No se implementa comunicación entre servicios en esta fase.

## RBAC

`auth-service` owns role persistence and includes effective roles in JWTs.
Gateway validates and sanitizes the role claim before propagating it as an
internal header. Gateway authentication is not a replacement for authorization:
each downstream service must enforce role permissions and resource ownership.
Only the three official roles are accepted. See `RBAC.md` for the complete
contract.
