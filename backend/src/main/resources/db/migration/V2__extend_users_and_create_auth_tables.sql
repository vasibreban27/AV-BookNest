-- Keep V1 intact: this migration upgrades the initial users table without
-- invalidating a Flyway checksum that may already be stored in Neon.

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT chk_roles_name CHECK (name IN ('USER', 'ADMIN'))
);

INSERT INTO roles (name) VALUES ('USER'), ('ADMIN');

ALTER TABLE users
    ALTER COLUMN email SET NOT NULL,
    ADD COLUMN first_name VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN last_name VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN password_hash VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN role_id BIGINT,
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT uk_users_email UNIQUE (email),
    ADD CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id);

UPDATE users
SET role_id = (SELECT id FROM roles WHERE name = 'USER')
WHERE role_id IS NULL;

ALTER TABLE users
    ALTER COLUMN role_id SET NOT NULL,
    ALTER COLUMN first_name DROP DEFAULT,
    ALTER COLUMN last_name DROP DEFAULT,
    ALTER COLUMN password_hash DROP DEFAULT;

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_email_verification_tokens_token UNIQUE (token),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_password_reset_tokens_token UNIQUE (token),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_email_verification_tokens_user_id ON email_verification_tokens (user_id);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);
