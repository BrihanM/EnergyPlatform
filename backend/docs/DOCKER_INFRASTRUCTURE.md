# Infraestructura Docker local

## Arquitectura

`docker-compose.yml` ejecuta PostgreSQL y Redis, sin contenerizar todavía
`api-gateway` ni `auth-service`.

| Servicio | Imagen fijada | Puerto local | Uso |
|---|---|---:|---|
| PostgreSQL | `postgres:17.6-alpine3.22` | `${POSTGRES_PORT}` -> 5432 | Base relacional compartida |
| Redis | `redis:8.2.1-alpine3.22` | `${REDIS_PORT}` -> 6379 | Caché y almacenamiento temporal |

Ambos servicios están en la red interna `mi-app-backend-internal`. Los puertos
se enlazan sólo a `127.0.0.1`. Cuando los microservicios se incorporen al
Compose, deberán conectarse usando `postgres` y `redis`, no `localhost`.

## Variables de entorno

```powershell
Copy-Item .env.example .env
```

Editar `.env` y sustituir los marcadores de `POSTGRES_PASSWORD` y
`REDIS_PASSWORD`. El archivo `.env` está excluido de Git y no debe contener
credenciales reutilizadas en entornos compartidos o productivos.

## Persistencia y seguridad

- `mi-app-postgres-data` mantiene los datos de PostgreSQL.
- `mi-app-redis-data` mantiene AOF y snapshots de Redis.
- PostgreSQL se inicializa sólo con sus variables estándar; no se crean tablas
  ni scripts de negocio.
- Redis requiere contraseña, está en modo protegido y usa AOF con snapshots.
- Los health checks usan `pg_isready` y `redis-cli` autenticado.
- Ambos servicios usan `restart: unless-stopped`.

PostgreSQL es una instancia única en esta fase, pero cada microservicio debe
mantener propiedad lógica de sus datos. Las migraciones Flyway se añadirán
posteriormente. Redis es opcional para la lógica fundamental del dominio.

## Operación

Ejecutar desde `backend`:

```powershell
docker compose pull
docker compose up -d
docker compose ps
docker inspect --format='{{json .State.Health}}' mi-app-postgres
docker inspect --format='{{json .State.Health}}' mi-app-redis
docker compose logs -f postgres
docker compose logs -f redis
docker compose stop
docker compose start
docker compose restart
docker compose up -d --force-recreate
docker compose exec postgres psql -U "$env:POSTGRES_USER" -d "$env:POSTGRES_DB"
docker compose exec redis redis-cli --no-auth-warning -a "$env:REDIS_PASSWORD" ping
docker compose down
```

Para eliminar contenedores y también los datos persistidos:

```powershell
docker compose down -v
```

Este último comando es destructivo. Para `psql` y `redis-cli`, cargar primero
las variables de `.env` en la sesión PowerShell o sustituirlas por valores
locales sin registrar secretos en historiales compartidos.

## Desarrollo frente a producción

Esta configuración es para desarrollo local: expone puertos sólo en loopback,
usa secretos desde `.env` y emplea una red Docker bridge. En producción se
deben usar un gestor de secretos, redes privadas, TLS, backups, monitorización,
límites de recursos, actualización controlada de imágenes y firewall. No se
debe publicar PostgreSQL o Redis directamente a Internet.

## Solución de problemas

- Si Compose no puede conectarse, iniciar Docker Desktop con el motor Linux.
- Si un puerto está ocupado, cambiar `POSTGRES_PORT` o `REDIS_PORT` en `.env`.
- Si se cambian credenciales después de la primera inicialización, el volumen
  existente conserva la configuración original; aplicar un procedimiento de
  rotación o recrear explícitamente el volumen.
- Revisar `docker compose logs postgres` y `docker compose logs redis`.
