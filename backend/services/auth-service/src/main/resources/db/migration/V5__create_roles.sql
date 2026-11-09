CREATE TABLE roles (
    code VARCHAR(32) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (code),
    CONSTRAINT ck_roles_code CHECK (code IN ('NORMAL_USER', 'ADMIN', 'SECURITY_AUDITOR'))
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_code)
        REFERENCES roles (code) ON DELETE RESTRICT
);

CREATE INDEX idx_user_roles_role_code ON user_roles (role_code);

INSERT INTO roles (code) VALUES
    ('NORMAL_USER'),
    ('ADMIN'),
    ('SECURITY_AUDITOR');
