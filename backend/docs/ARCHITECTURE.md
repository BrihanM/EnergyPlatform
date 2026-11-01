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

En desarrollo, Docker Compose levanta PostgreSQL y Redis, mientras las
aplicaciones se ejecutan desde Maven. En producción, Gateway y Auth Service
deben conectarse a una red interna y sólo Gateway debe aceptar tráfico externo.

## Hexagonal

- `domain`: entidades, objetos de valor y reglas de negocio puras; no depende de Spring, PostgreSQL, Redis ni otras tecnologías.
- `application`: casos de uso, servicios de aplicación y puertos de entrada/salida; coordina el dominio sin conocer implementaciones concretas.
- `infrastructure`: adaptadores de entrada y salida, persistencia, integraciones y configuración técnica; implementa los puertos internos.

La dependencia apunta hacia el interior: infrastructure -> application -> domain. Los controladores y repositorios no contienen reglas de negocio.

## Responsabilidades transversales

El Gateway es el punto de entrada externo, encargado de routing, validación
JWT, filtros, correlation IDs, metadatos de auditoría y controles perimetrales.
No tiene reglas de negocio. La auditoría nunca registra contraseñas, tokens,
credenciales ni datos sensibles.

La comunicación futura usará contratos explícitos y mecanismos desacoplados. No se implementa comunicación entre servicios en esta fase.
