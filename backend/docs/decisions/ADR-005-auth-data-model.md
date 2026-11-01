# ADR-005: Modelo inicial de identidad y acceso

## Estado
Aceptada

## Decisión

El contexto de autenticación separa `users` y `accounts`. Un `User` representa
la identidad general y una `Account` representa una identidad de acceso con su
identificador, estado y hash de credencial.

La relación inicial es uno a muchos: una identidad puede tener varias cuentas
de acceso, mientras que cada cuenta pertenece a una única identidad. El
`login_identifier` es único dentro del contexto de autenticación.

Se utilizan UUID generados por el dominio. Así los identificadores pueden
crearse sin depender de una secuencia central y son adecuados para despliegues
distribuidos. Las entidades JPA y la migración Flyway utilizan la misma
estrategia.

## Consecuencias

- El dominio permanece independiente de JPA, Spring y PostgreSQL.
- `password_hash` se almacena separado de los datos generales y nunca representa
  una contraseña en texto plano.
- Flyway controla el esquema; Hibernate sólo valida la estructura mediante
  `ddl-auto=validate`.
- La instancia PostgreSQL puede ser compartida, pero `auth-service` es el
  propietario lógico de estas tablas.
