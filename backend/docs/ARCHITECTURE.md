# Arquitectura

La plataforma es un monorepo Maven de microservicios. Cada servicio posee un bounded context, lógica de negocio y persistencia independientes, y puede evolucionar y desplegarse sin acoplarse al resto.

## Hexagonal

- `domain`: entidades, objetos de valor y reglas de negocio puras; no depende de Spring, PostgreSQL, Redis ni otras tecnologías.
- `application`: casos de uso, servicios de aplicación y puertos de entrada/salida; coordina el dominio sin conocer implementaciones concretas.
- `infrastructure`: adaptadores de entrada y salida, persistencia, integraciones y configuración técnica; implementa los puertos internos.

La dependencia apunta hacia el interior: infrastructure -> application -> domain. Los controladores y repositorios no contienen reglas de negocio.

## Responsabilidades transversales

El Gateway será el punto de entrada externo, encargado de routing, filtros, correlation IDs, metadatos de auditoría y controles perimetrales. No tendrá reglas de negocio. La auditoría nunca registrará contraseñas, tokens, credenciales ni datos sensibles.

La comunicación futura usará contratos explícitos y mecanismos desacoplados. No se implementa comunicación entre servicios en esta fase.
