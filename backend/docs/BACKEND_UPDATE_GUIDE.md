# Guía de actualización del backend y los contenedores

Este documento describe cómo aplicar los cambios que se implementan en el backend (código Java, migraciones Flyway, configuración y scripts) y cómo reconstruir y redesplegar los contenedores de Docker Compose sin perder datos.

Ruta de trabajo: `backend/`. Todos los comandos se ejecutan desde esa carpeta.

## 1. Servicios y componentes

| Servicio (`docker-compose.yml`) | Qué contiene | Imagen construida |
|---|---|---|
| `postgres` | PostgreSQL con volumen `postgres-data` | No (imagen oficial) |
| `redis` | Redis con volumen `redis-data` | No (imagen oficial) |
| `postgres-bootstrap` | Crea roles y bases de datos; se ejecuta una vez | No |
| `auth-service` | Autenticación, usuarios, auditoría de Auth | Sí |
| `energy-market-service` | Ofertas, dispositivos, lecturas, transacciones | Sí |
| `api-gateway` | Punto de entrada HTTP | Sí |
| `demo-seed` | Carga de datos de desarrollo (perfil `seed`) | No (`python:3.12-slim`, script montado) |

Redes: `backend-internal` (interna, sin acceso externo) y `gateway-edge` (solo el gateway).

Volúmenes persistentes: `postgres-data` y `redis-data`. **No se borran al reconstruir los contenedores.** Solo se borran con `down -v`.

## 2. Qué hacer según el tipo de cambio

| Tipo de cambio | Acción mínima | Reinicia datos |
|---|---|---|
| Código Java de un servicio | Reconstruir y recrear ese servicio | No |
| Nueva migración Flyway (`V<n>__*.sql`) | Reconstruir y recrear el servicio dueño; Flyway la aplica al arrancar | No |
| Variable de entorno nueva o cambiada | Añadirla a `.env` y recrear el servicio afectado | No |
| Cambio en `docker-compose.yml` | `up -d` del servicio afectado (Compose recrea solo lo que cambió) | No |
| Cambio en `application.yml` | Reconstruir el servicio (el YAML va dentro de la imagen) | No |
| Cambio en `infrastructure/seed/seed_demo_data.py` | Volver a ejecutar `demo-seed` (el script está montado, no requiere build) | No |
| Cambio en `postgres-bootstrap` o en contraseñas de roles | Ver sección 6 (riesgo) | Puede |
| Cambio de `AUTH_PII_ENCRYPTION_KEY` | Ver sección 7 (no es un cambio simple) | Sí |

## 3. Actualizar un servicio tras cambios de código

Ejemplo con `auth-service` (sustituir por el servicio modificado):

```powershell
cd backend
docker compose build auth-service
docker compose up -d --no-deps auth-service
docker compose ps auth-service
docker compose logs --tail 100 auth-service
```

- `--no-deps` evita recrear dependencias que no cambiaron.
- Espera a que `ps` muestre el estado `healthy` antes de seguir.
- Si el servicio no arranca, revisa `logs`. Las migraciones Flyway se registran al inicio.

Para reconstruir varios servicios a la vez:

```powershell
docker compose build auth-service energy-market-service api-gateway
docker compose up -d auth-service energy-market-service api-gateway
```

## 4. Migraciones Flyway

- Las migraciones se encuentran en `services/<servicio>/src/main/resources/db/migration/`.
- **No modificar migraciones ya aplicadas** (por ejemplo `V1` a `V8`). Crear siempre una nueva versión (`V9__descripcion.sql`).
- Flyway se ejecuta al arrancar el servicio dueño. Cada servicio tiene su propio esquema y su propio historial; no se mezclan.
- Antes de desplegar una migración, revisar que sea compatible con los registros existentes (sin pérdida de datos).
- Verificar el historial después del despliegue:

```powershell
docker compose exec postgres psql -U postgres -d <base_del_servicio> -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Si una migración falla, `flyway_schema_history` queda con `success = false`. Corregir la causa en una **nueva** migración o restaurar el estado previo; no editar la migración fallida.

## 5. Variables de entorno

1. Añadir o cambiar la variable en `.env` (copiar el nombre desde `.env.example`).
2. Si la variable se usa en `docker-compose.yml`, añadirla allí también.
3. Recrear únicamente el servicio afectado:

```powershell
docker compose up -d --no-deps auth-service
```

Nunca guardar secretos en `.env.example`, en el código ni en la base de datos. El fichero `.env` no debe subirse al repositorio.

## 6. Cambios que afectan a Postgres

`postgres-bootstrap` crea roles y ajusta contraseñas. Si cambian las contraseñas de `AUTH_*` o `ENERGY_MARKET_*` en `.env`, el bootstrap puede reajustar los roles existentes en el volumen. Antes de ejecutarlo:

1. Hacer copia de seguridad (ver sección 8).
2. Confirmar que las credenciales nuevas coinciden con las que usan los servicios.
3. Ejecutar `docker compose up -d postgres-bootstrap` y revisar sus logs.

## 7. Cambio de la clave de cifrado (`AUTH_PII_ENCRYPTION_KEY`)

Los nombres y apellidos de usuario están cifrados con `pgcrypto` (F18). **Cambiar la clave sin re-cifrar los datos deja esos registros ilegibles.** Procedimiento:

1. Parar los servicios que leen usuarios: `docker compose stop auth-service`.
2. Hacer copia de seguridad.
3. Re-cifrar los datos con la clave nueva (no existe una herramienta automatizada en el testbed; documentar cualquier procedimiento antes de aplicarlo).
4. Actualizar `.env` y recrear el servicio.

Si no es posible re-cifrar, la alternativa es `down -v` y recargar datos (pérdida total).

## 8. Copias de seguridad antes de cambios de riesgo

```powershell
cd backend
docker compose exec -T postgres pg_dumpall -U postgres > backup_$(Get-Date -Format yyyyMMdd_HHmm).sql
```

Guardar el archivo fuera del repositorio. No commitear copias de seguridad.

## 9. Carga de datos de desarrollo (F19)

Después de que `auth-service`, `energy-market-service` y `api-gateway` estén `healthy`:

```powershell
docker compose --profile seed up -d demo-seed
docker compose --profile seed logs demo-seed
```

La carga es idempotente. Para volver a cargar datos de prueba desde cero, ver [DEV_SEED_DATA.md](./DEV_SEED_DATA.md). El perfil `seed` nunca debe usarse en producción.

Si cambia `seed_demo_data.py`, basta con repetir el comando anterior.

## 10. Reinicialización completa del entorno de pruebas

Esta operación **borra todos los datos** de Postgres y Redis:

```powershell
cd backend
docker compose down -v
docker compose up -d --build
docker compose --profile seed up -d demo-seed   # opcional
```

## 11. Verificación tras cada despliegue

```powershell
docker compose ps
docker compose logs --tail 50 api-gateway auth-service energy-market-service
```

Comprobaciones mínimas:
- Todos los servicios con estado `healthy` o `running`.
- Login de un usuario existente en `POST /api/v1/auth/login` a través del gateway.
- Las migraciones aparecen como `success = true` en `flyway_schema_history`.
- Ningún log muestra contraseñas, claves de cifrado ni credenciales de dispositivos.

Para cambios de código, ejecutar las pruebas del módulo afectado antes de desplegar:

```powershell
cd services\auth-service; mvn -B test
cd ..\energy-market-service; mvn -B test
```

## 12. Problemas frecuentes

| Síntoma | Causa probable | Acción |
|---|---|---|
| Compose falla con `variable is not set` | Falta una variable en `.env` | Copiarla desde `.env.example` |
| Servicio en `unhealthy` | Migración fallida o BD no disponible | Revisar `logs` y `flyway_schema_history` |
| Cambio de código no visible | Imagen sin reconstruir | `docker compose build <servicio>` y luego `up -d` |
| Imagen antigua tras `build` | Caché de capas | `docker compose build --no-cache <servicio>` |
| Errores de conexión entre servicios | Red o nombre de servicio distinto | Revisar `backend-internal` y los nombres en `docker-compose.yml` |
| Gateway responde 502 | Servicio destino caído | `docker compose ps` y `logs` del servicio destino |

## 13. Puntos de control

- `docker image prune` elimina imágenes sin uso. No afecta a volúmenes.
- No usar `docker volume prune` sin confirmar: borra datos persistentes.
- Antes de un cambio de esquema o de clave, hacer una copia de seguridad (sección 8).
