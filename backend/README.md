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

El Gateway está preparado para Spring Cloud Gateway y expone la ruta configurable
`/api/v1/auth/**`, propaga
`X-Correlation-ID`, emite auditoría técnica estructurada y aplica CORS y
headers de seguridad. No implementa JWT ni autenticación todavía.

Consulta `docs/` para las decisiones y convenciones obligatorias, y
`docs/API_GATEWAY.md` para su operación.
