# Guía de demostración del testbed EnergyProsumer

Esta guía describe la ejecución local del testbed académico con PostgreSQL,
Redis, API Gateway, Auth Service, Energy Market Service y el frontend React.
Está destinada exclusivamente a desarrollo y demostraciones. No se debe usar el
perfil de seed ni las cuentas descritas aquí en producción.

## A. Preparación del entorno

### Requisitos

- Docker Desktop con el motor Linux y Docker Compose v2.
- Git y PowerShell en Windows.
- Node.js y npm para ejecutar el frontend.
- Un puerto libre para el Gateway (`8080`) y para Vite (`5173`).
- Este repositorio clonado y una terminal ubicada en su raíz.

### Variables y secretos locales

El backend lee `backend/.env`; la plantilla completa está en
`backend/.env.example`. No se deben copiar secretos reales al repositorio.
Como mínimo, el archivo debe contener valores locales para:

`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `REDIS_PASSWORD`,
`AUTH_DB_USERNAME`, `AUTH_DB_PASSWORD`, `AUTH_FLYWAY_USERNAME`,
`AUTH_FLYWAY_PASSWORD`, `ENERGY_MARKET_DB_USERNAME`,
`ENERGY_MARKET_DB_PASSWORD`, `ENERGY_MARKET_FLYWAY_USERNAME`,
`ENERGY_MARKET_FLYWAY_PASSWORD`, `AUTH_PII_ENCRYPTION_KEY`, `JWT_ISSUER`,
`JWT_SECRET`, `AUTH_INTROSPECTION_SECRET`, `JWT_EXPIRATION`,
`REFRESH_TOKEN_EXPIRATION` y las variables `CORS_*`.

Para la demostración, el seed exige además:

```dotenv
DEV_SEED_ENABLED=true
SEED_DEMO_PASSWORD=<contraseña-local-de-al-menos-12-caracteres>
SEED_DEMO_ADMIN_USERNAME=demo-admin
SEED_AUDITOR_USERNAME=demo-auditor
SEED_NORMAL_USERS=8
SEED_DEVICES=4
SEED_READINGS_PER_DEVICE=10
SEED_OFFERS_TOTAL=20
SEED_CANCELLED_OFFERS=2
SEED_TRANSACTIONS=10
```

La configuración utilizada en esta validación tiene `SEED_DEMO_PASSWORD`
establecida (28 caracteres), pero su valor no se publica en esta guía ni se
versiona. El operador debe introducir la contraseña que ya está en su
`backend/.env`. Es la misma para todas las cuentas de demostración.

El Gateway permite el origen `http://localhost:5173`, los métodos
`GET,POST,PUT,PATCH,DELETE,OPTIONS`, las credenciales CORS y cookies de
refresh no seguras con `SameSite=Lax`, configuración adecuada para HTTP local.

### Levantar el backend

Desde `backend\`:

```powershell
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

El orden aplicado por Compose es:

1. PostgreSQL y Redis.
2. `postgres-bootstrap`, que crea los roles separados de ejecución y Flyway.
3. Auth Service, después de PostgreSQL y el bootstrap.
4. Energy Market Service, después de Auth Service saludable.
5. API Gateway, después de Auth y Market saludables.

Sólo se publica el Gateway:

- Gateway: <http://localhost:8080>
- Health del Gateway: <http://localhost:8080/actuator/health>
- Frontend Vite: <http://localhost:5173>

Auth Service, Energy Market Service, PostgreSQL y Redis permanecen en la red
Docker interna. Los volúmenes `mi-app-postgres-data` y `mi-app-redis-data` se
conservan al reiniciar contenedores. No ejecutar `docker compose down -v`
durante una demostración: elimina la base de datos.

### Migraciones y seed

Las migraciones se ejecutan automáticamente al iniciar Auth y Market. Se pueden
comprobar con:

```powershell
docker compose logs --no-color auth-service energy-market-service
docker compose ps
```

El historial Flyway de Auth queda en `public.flyway_schema_history` y el de
Market en `energy_market.flyway_schema_history`. Para cargar los datos:

```powershell
docker compose --profile seed up --build -d demo-seed
docker compose --profile seed wait demo-seed
docker compose --profile seed logs --no-color demo-seed
```

El último mensaje esperado es `[seed] done`. El seed es idempotente: una segunda
ejecución debe informar cero registros nuevos. El primer administrador se crea
en Auth Service cuando `DEV_SEED_ENABLED=true`; el resto de datos se crea por
la API pública a través del Gateway.

### Ejecutar el frontend

Desde `frontend\`:

```powershell
Copy-Item .env.example .env
npm install
npm run dev
```

El archivo `frontend/.env` debe contener:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=Mi App Frontend
```

El frontend envía el access token en `Authorization: Bearer ...` y las
credenciales de cookie con `credentials: include`. Si el puerto del Gateway
cambia, actualizar `VITE_API_BASE_URL` y `GATEWAY_PORT` de forma consistente.

### Comprobación inicial

```powershell
docker compose ps
curl.exe http://localhost:8080/actuator/health
```

Los cuatro servicios deben mostrar `healthy`; el seed debe mostrar estado
`Exited (0)` después de terminar. No se debe tomar como señal de salud el
acceso directo a `8081` o `8082`, porque no están publicados al host.

## B. Cuentas de demostración

El seed crea las cuentas siguientes. La contraseña real es el valor local de
`SEED_DEMO_PASSWORD`; no se repite aquí para evitar publicar un secreto,
aunque sea de laboratorio.

| Cuenta | Usuario | Contraseña | Función |
|---|---|---|---|
| Administrador | `demo-admin` | `SEED_DEMO_PASSWORD` local | Administración de usuarios, roles y medidores |
| Auditor | `demo-auditor` | `SEED_DEMO_PASSWORD` local | Consulta exclusiva de auditoría |
| Vendedor | `demo-user-01` | `SEED_DEMO_PASSWORD` local | Publicación y gestión de sus ofertas |
| Comprador | `demo-user-02` | `SEED_DEMO_PASSWORD` local | Consulta del marketplace y compra |
| Usuarios adicionales | `demo-user-03` a `demo-user-08` | `SEED_DEMO_PASSWORD` local | Pruebas de aislamiento y roles `NORMAL_USER` |

El seed asigna `NORMAL_USER` a las cuentas normales y también al auditor para
permitirle usar el flujo de lectura del marketplace si se requiere. El auditor
recibe además `SECURITY_AUDITOR`; no se debe confundir ese permiso con
`ADMIN`. El seed valida las credenciales con `POST /api/v1/auth/login` y no
imprime tokens ni contraseñas.

## C. Recorrido completo

La aplicación frontend actualmente implementa pantallas para marketplace,
detalle de oferta, publicación, mis ofertas, compra, historial y detalle de
transacción. La administración de usuarios/medidores y la auditoría se
demuestran mediante sus endpoints protegidos o consultas SQL; no existen
pantallas frontend para esas funciones.

| Paso | Cuenta y acción | Resultado esperado y registro | Evidencia sugerida |
|---|---|---|---|
| 1 | `demo-user-01`; abrir `/login` e iniciar sesión | Se obtiene JWT y cookie de refresh; se muestra `Marketplace` | Captura del login exitoso y de la navegación autenticada; no capturar tokens |
| 2 | Vendedor; abrir `Marketplace` | Se listan ofertas activas con paginación y filtros | Captura de la lista y de la URL; registrar un `GET /api/v1/offers` |
| 3 | Vendedor; `Publish offer`, indicar cantidad, precio y vencimiento futuro | La oferta se crea y aparece `Offer published successfully` | Captura del formulario y del detalle; registrar `POST /api/v1/offers` y `OFFER_CREATED` |
| 4 | Vendedor o comprador; volver al marketplace | La oferta figura disponible con su identificador | Captura de la tarjeta y anotar el UUID de oferta |
| 5 | Vendedor; `Sign out`; comprador; iniciar sesión | Se destruye la sesión local del vendedor y el comprador ve el marketplace | Capturas separadas; registrar `LOGOUT` y `LOGIN_SUCCEEDED` |
| 6 | `demo-user-02`; abrir la oferta del vendedor | Se muestra el detalle y el panel de compra | Captura del detalle, cantidad y precio |
| 7 | Comprador; confirmar la compra | La respuesta devuelve `CONFIRMED` cuando la simulación está en `SUCCESS` | Captura del resultado; registrar `POST /api/v1/transactions` con `Idempotency-Key` |
| 8 | Comprador; observar el detalle de transacción | Se muestran `Verification: ACCEPTED`, fecha y `Simulation reference` | Captura de la verificación; registrar `PURCHASE_STARTED`, `PURCHASE_CONFIRMED` y `operator_verifications` |
| 9 | Comprador; abrir historial | La transacción aparece en `My transactions` | Captura de la fila y del estado; registrar `GET /api/v1/transactions/me` |
| 10 | Comprador; abrir `View transaction details` | Se muestra UUID, oferta, rol, energía, precio, total y fechas | Captura del detalle; registrar `GET /api/v1/transactions/{id}` |
| 11 | Vendedor; iniciar sesión y abrir su historial | El vendedor puede consultar la transacción como participante y ve su rol `Seller` | Captura del detalle desde la cuenta vendedora; no exponer tokens |
| 12 | `demo-admin`; consultar `GET /api/v1/admin/users?page=0&size=20` y endpoints de medidores | Se listan usuarios sin credenciales y se pueden demostrar operaciones de dispositivos autorizadas | Captura de respuesta sin datos secretos y del evento administrativo |
| 13 | `demo-auditor`; consultar `GET /api/v1/auth/audit/events?page=0&size=20` y `GET /api/v1/market/audit/events?page=0&size=20` | Se muestran eventos de autenticación y mercado; un ADMIN ordinario recibe `403` | Capturas de ambos listados, filtros y timestamps; conservar los UUID de correlación |

La compra usa una simulación local de transferencia; no representa una
transferencia bancaria real. El modo configurado por defecto es `SUCCESS`.

## D. Seis capas de seguridad

| Etapas | Capas involucradas | Control comprobable | Evidencia |
|---|---|---|---|
| Login, logout y refresh | Identidad, Red, Monitoreo | JWT, roles, cookie de refresh, CORS del Gateway y eventos de sesión | Respuestas de login/logout, cookie sin valor visible, `LOGIN_SUCCEEDED`, `LOGOUT` |
| Marketplace y publicación | Identidad, Entradas, Red, Monitoreo | Autorización `NORMAL_USER`, validación de cantidad/precio/vencimiento, Gateway y auditoría | `201`, oferta creada y `OFFER_CREATED`; prueba negativa con datos inválidos |
| Compra y verificación | Identidad, Entradas, Monitoreo | Participación autorizada, clave de idempotencia, reglas de dominio y simulador de operador | `CONFIRMED`, `ACCEPTED`, `operator_verifications` y eventos de compra |
| Historial y detalle | Identidad, Entradas, Monitoreo | Consulta asociada al usuario autenticado, UUID y respuestas de error | Historial de comprador/vendedor y prueba con un tercer usuario |
| Usuarios y medidores | Identidad, Datos, Infraestructura, Monitoreo | Sólo `ADMIN`; roles SQL separados; credenciales de medidor no se almacenan en logs | `403` para NORMAL_USER y eventos de administración |
| Auditoría | Identidad, Datos, Infraestructura, Monitoreo | Sólo `SECURITY_AUDITOR`; Auth y Market mantienen tablas separadas con grants de inserción/lectura | Respuestas de ambos endpoints y consultas SQL de auditoría |
| Todo el despliegue local | Datos, Red, Infraestructura | Hash de contraseñas, pgcrypto para nombres, red interna, variables externas y volúmenes persistentes | Historial Flyway, configuración Compose, volumen conservado y ausencia de secretos en logs |

### Controles activos frente a controles futuros

Están activos en este testbed: JWT y autorización por roles, refresh token
rotado, hashing de contraseñas, cifrado pgcrypto de nombres, protección de
credenciales de medidores, validaciones de API, consultas parametrizadas,
Gateway, CORS, red Docker interna, separación de roles SQL, auditoría y
volúmenes persistentes.

HTTPS no está activo en esta ejecución: el frontend y el Gateway usan HTTP en
`localhost`. La protección TLS del despliegue, rotación operativa de secretos,
retención y respaldo de auditoría, rate limiting, monitoreo externo y
aislamiento de las migraciones en un job separado son controles de despliegue
futuro u operación pendiente. No deben presentarse como demostrados por este
entorno local.

## E. Pruebas negativas

Ejecutar las siguientes solicitudes con tokens obtenidos por login, sin
publicarlos en capturas:

| Prueba | Solicitud | Resultado esperado |
|---|---|---|
| NORMAL_USER en función ADMIN | `GET /api/v1/admin/users?page=0&size=5` con `demo-user-01` | `403 Forbidden` |
| ADMIN en auditoría | `GET /api/v1/auth/audit/events?page=0&size=5` con `demo-admin` | `403 Forbidden` |
| Modificar oferta ajena | `PUT /api/v1/offers/{id-ajena}` con payload válido y usuario no propietario | `404 Not Found` en la implementación actual, sin modificación |
| Consultar transacción ajena | `GET /api/v1/transactions/{id-ajena}` con un NORMAL_USER no participante | Debe comprobarse en cada versión; en esta ejecución devolvió `500`, limitación que debe reportarse y no ocultarse |
| Credencial incorrecta | `POST /api/v1/auth/login` con contraseña incorrecta | `401 Unauthorized` y evento `LOGIN_FAILED` |
| Datos inválidos | `POST /api/v1/offers` con cantidad negativa, precio cero o vencimiento inválido | `400 Bad Request`, sin oferta creada |

La prueba de recurso ajeno debe realizarse con `demo-user-03` contra una
transacción de `demo-user-01`/`demo-user-02`. El `500` observado en la
validación no se debe interpretar como autorización correcta; debe quedar como
incidencia pendiente antes de una defensa que exija aislamiento por `403` o
`404`.

## F. Cierre y verificación SQL

### Consultas útiles

Ejecutar desde `backend\` usando las credenciales locales, sin escribir
contraseñas en el historial:

```powershell
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

```sql
-- Ofertas y estado
SELECT id, seller_id, energy_amount_kwh, price_per_kwh, status, expires_at
FROM energy_market.energy_offers
ORDER BY created_at DESC;

-- Transacciones y verificación del operador
SELECT t.id, t.offer_id, t.seller_id, t.buyer_id, t.status,
       v.result, v.reason, v.simulation_reference, v.verified_at
FROM energy_market.energy_transactions t
LEFT JOIN energy_market.operator_verifications v ON v.transaction_id = t.id
ORDER BY t.created_at DESC;

-- Eventos de Auth y Market
SELECT occurred_at, event_type, actor_type, actor_id, result
FROM public.auth_audit_events
ORDER BY occurred_at DESC, id DESC
LIMIT 30;

SELECT occurred_at, event_type, actor_type, actor_id, resource_type,
       resource_id, result
FROM energy_market.audit_events
ORDER BY occurred_at DESC, id DESC
LIMIT 30;

-- Migraciones aplicadas
SELECT installed_rank, version, description, success
FROM public.flyway_schema_history
ORDER BY installed_rank;

SELECT installed_rank, version, description, success
FROM energy_market.flyway_schema_history
ORDER BY installed_rank;
```

### Evidencias recomendadas

- `docker compose ps` con PostgreSQL, Redis, Auth, Market y Gateway
  saludables.
- Logs de migración sin errores y salida final de `demo-seed`.
- Capturas de login, marketplace, oferta, compra, verificación, historial y
  detalle desde vendedor y comprador.
- Respuestas de `403`, `401` y `400` de las pruebas negativas.
- Listados de usuarios y dispositivos usando ADMIN, sin credenciales de
  medidores.
- Listados de auditoría Auth y Market usando SECURITY_AUDITOR.
- Consultas SQL de ofertas, transacciones, verificaciones y eventos.
- Identificadores de correlación y UUID de oferta/transacción, nunca JWT,
  cookies, contraseñas o credenciales de medidores.

### Estado validado de esta ejecución

Se construyeron las tres imágenes y el stack quedó saludable con los
volúmenes existentes. Flyway terminó correctamente en Auth (V1–V8 más grants
repetibles) y Market (V1–V5 más grants repetibles). El seed quedó disponible e
idempotente; los conteos base fueron 10 usuarios, 4 medidores, 40 lecturas,
20 ofertas y 10 transacciones, y las pruebas de flujo añadieron datos de
validación. Los cuatro logins devolvieron `200` con los roles esperados.

El frontend pasó `npm run build`, `npm run lint` y sus 57 pruebas (`npm run
test -- --run`). El flujo real de validación publicó una oferta (`201`) y la
compró (`200`, `CONFIRMED`, `ACCEPTED`); el auditor consultó ambos listados
(`200`). La interfaz no incluye administración ni auditoría, por lo que esas
partes requieren API/SQL. La consulta de una transacción completamente ajena
devolvió `500` en esta versión y debe considerarse una limitación conocida.
