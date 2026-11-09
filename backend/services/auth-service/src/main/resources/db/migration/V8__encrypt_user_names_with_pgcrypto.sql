-- HU-18: cifrado de nombres de usuario (datos personales) con pgcrypto.
-- La clave llega por placeholder de Flyway desde AUTH_PII_ENCRYPTION_KEY; nunca se almacena en la base de datos.
-- El correo electrónico no se cifra aquí: login, unicidad y búsqueda dependen de él (pendiente de índice HMAC).
-- Las columnas en claro se eliminan tras el backfill; no hay rollback automático (Flyway Community).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE users
    ADD COLUMN first_name_enc BYTEA,
    ADD COLUMN last_name_enc BYTEA;

UPDATE users
SET first_name_enc = pgp_sym_encrypt(first_name, '${piiEncryptionKey}'),
    last_name_enc = pgp_sym_encrypt(last_name, '${piiEncryptionKey}');

ALTER TABLE users
    ALTER COLUMN first_name_enc SET NOT NULL,
    ALTER COLUMN last_name_enc SET NOT NULL,
    DROP COLUMN first_name,
    DROP COLUMN last_name;
