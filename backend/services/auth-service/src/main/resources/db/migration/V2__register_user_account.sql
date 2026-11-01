ALTER TABLE users ADD COLUMN first_name VARCHAR(100);
ALTER TABLE users ADD COLUMN last_name VARCHAR(100);
UPDATE users SET first_name = display_name, last_name = display_name;
ALTER TABLE users ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN last_name SET NOT NULL;
ALTER TABLE users DROP COLUMN display_name;

ALTER TABLE accounts ADD COLUMN username VARCHAR(50);
ALTER TABLE accounts ADD COLUMN email VARCHAR(320);
UPDATE accounts SET username = login_identifier, email = login_identifier;
UPDATE accounts SET username = LOWER(username), email = LOWER(email);
ALTER TABLE accounts ALTER COLUMN username SET NOT NULL;
ALTER TABLE accounts ALTER COLUMN email SET NOT NULL;
ALTER TABLE accounts DROP CONSTRAINT uk_accounts_login_identifier;
ALTER TABLE accounts DROP COLUMN login_identifier;

ALTER TABLE accounts ADD CONSTRAINT ck_accounts_username_normalized
    CHECK (username = LOWER(username));
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_email_normalized
    CHECK (email = LOWER(email));
CREATE UNIQUE INDEX uk_accounts_username ON accounts (username);
CREATE UNIQUE INDEX uk_accounts_email ON accounts (email);
