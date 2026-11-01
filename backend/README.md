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

## Inicio con Docker Compose

Requisitos: Java 21 y Docker Desktop con Compose. Copia la plantilla de
variables y levanta todo el entorno:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

El único puerto publicado es el Gateway en `http://localhost:8080`.
`auth-service`, PostgreSQL y Redis sólo son accesibles en la red Docker interna.
Los health checks de Compose esperan a PostgreSQL y a Auth Service antes de
iniciar el siguiente servicio. Para validar el build localmente:

```powershell
.\mvnw.cmd clean verify
docker compose config --quiet
```

El flujo es: `register` -> `login` (JWT + cookie de refresh) -> solicitud
protegida -> `refresh` (rotación) -> `logout`. La ruta técnica protegida
`GET /api/v1/integration/probe` permite verificar forwarding, identidad derivada
del JWT y `X-Correlation-ID` sin introducir lógica de negocio.

Consulta `docs/` para las decisiones y convenciones obligatorias, y
`docs/API_GATEWAY.md` para su operación.
