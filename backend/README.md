# Mi App Backend

Repositorio Maven multi-módulo para los microservicios de Mi App. La solución inicia con `api-gateway` y `auth-service`; cada aplicación se construye y despliega de forma independiente.

## Requisitos

- Java 21 LTS
- Maven Wrapper (`mvnw`/`mvnw.cmd`)

## Comandos

```shell
./mvnw clean verify
./mvnw -pl services/api-gateway clean verify
./mvnw -pl services/auth-service clean verify
```

El Gateway usa Spring Cloud Gateway WebFlux, valida los access tokens JWT
emitidos por `auth-service`, protege las rutas no públicas, elimina headers de
identidad enviados por clientes y propaga únicamente la identidad validada.
También propaga `X-Correlation-ID`, emite auditoría técnica estructurada y
aplica CORS y headers de seguridad.

## Inicio local

Requisitos: Java 21 y Docker Desktop con Compose. Para iniciar PostgreSQL y
Redis:

```powershell
Copy-Item .env.example .env
docker compose up -d
.\mvnw.cmd clean verify
```

Inicia cada aplicación desde su módulo con el mismo `.env` cargado en el
entorno. El Gateway queda en `http://localhost:8080` y `auth-service` en
`http://localhost:8081`; en producción los microservicios deben permanecer en
una red privada y sólo el Gateway debe exponerse públicamente.

El flujo es: `register` -> `login` (JWT + cookie de refresh) -> solicitud
protegida -> `refresh` (rotación) -> `logout`. Las solicitudes desde navegador
que usen cookies deben habilitar `withCredentials: true`.

Consulta `docs/` para las decisiones y convenciones obligatorias, y
`docs/API_GATEWAY.md` para su operación.
