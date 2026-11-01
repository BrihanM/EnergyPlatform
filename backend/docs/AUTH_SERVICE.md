# Auth Service

## Responsabilidad y arquitectura

`auth-service` es responsable de la identidad de usuario y de las cuentas
utilizadas para acceder a la plataforma. Está organizado con arquitectura
hexagonal: el caso de uso vive en `application`, las reglas y modelos puros en
`domain`, y los adaptadores REST, JPA y de hashing en `infrastructure`.

## Registro de usuario

El caso de uso `RegisterUserUseCase` recibe un `RegisterUserCommand`, valida las
reglas de negocio, normaliza los identificadores, comprueba duplicados, genera
el hash de contraseña, crea `User` y `Account`, y persiste ambos dentro de una
transacción. Las restricciones de PostgreSQL son la última barrera ante carreras
entre solicitudes concurrentes; sus violaciones se traducen a errores seguros.

`User` contiene `firstName`, `lastName`, `createdAt` y `updatedAt`. `Account`
contiene la relación por `userId`, `username`, `email`, `passwordHash`, estado y
fechas de auditoría. La contraseña original nunca se almacena ni se devuelve.

## Normalización y unicidad

`username` y `email` se recortan y convierten a minúsculas con `Locale.ROOT`
antes de consultar o persistir. El nombre de usuario acepta entre 3 y 50
caracteres alfanuméricos, puntos, guiones y guiones bajos, sin separadores al
inicio o al final. El correo usa validación HTTP de formato y una comprobación
de negocio equivalente.

La migración `V2__register_user_account.sql` crea restricciones que sólo
permiten valores ya normalizados y los índices únicos correspondientes. Así la
base de datos impide tanto duplicados exactos como escrituras directas en
mayúsculas que evadirían la política; también mantiene la unicidad cuando dos
solicitudes concurrentes llegan al mismo tiempo. La migración transforma las
columnas de la estructura inicial V1 sin modificarla.

## Credenciales

La política inicial exige una contraseña de 12 a 128 caracteres, con al menos
una letra y un número. El adaptador `PasswordHashingAdapter` usa
`BCryptPasswordEncoder`; el dominio y la aplicación dependen únicamente del
puerto `PasswordHashingPort`. Los comandos sobrescriben `toString()` para
redactar la contraseña.

## API

### `POST /api/v1/auth/register`

Request:

```json
{
  "firstName": "Brayan",
  "lastName": "Marín",
  "username": "brayan.marin",
  "email": "brayan@example.com",
  "password": "ContraseñaSegura1"
}
```

La respuesta es `201 Created`, incluye un header `Location` y devuelve sólo
datos seguros:

```json
{
  "id": "uuid",
  "firstName": "Brayan",
  "lastName": "Marín",
  "username": "brayan.marin",
  "email": "brayan@example.com",
  "status": "ACTIVE",
  "createdAt": "2026-10-31T00:00:00Z"
}
```

Los datos inválidos producen `400`, los identificadores duplicados `409`, y
los errores inesperados de persistencia se traducen a una respuesta segura.
El formato contiene `timestamp`, `status`, `error`, `message`, `path` y el
`X-Correlation-ID` recibido, cuando existe. No se incluyen SQL, stack traces,
hashes ni credenciales.

## Configuración y pruebas

La conexión se configura con `AUTH_DB_URL`, `AUTH_DB_USERNAME`,
`AUTH_DB_PASSWORD`, `POSTGRES_DB` y `POSTGRES_PORT`. Las pruebas unitarias
validan normalización, política de contraseña, duplicados y ausencia de
secretos. Las pruebas de persistencia usan PostgreSQL 18 mediante Testcontainers
cuando Docker está disponible y comprueban Flyway, relación y restricciones
únicas.

```powershell
Set-Location backend
.\mvnw.cmd -pl services/auth-service -am test
```

Login, JWT, sesiones, roles, recuperación y verificación de correo quedan fuera
de esta fase.
