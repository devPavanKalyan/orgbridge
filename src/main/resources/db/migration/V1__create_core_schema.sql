CREATE TABLE clients (
    id VARCHAR(36) PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    username VARCHAR(320) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    roles VARCHAR(500) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT
);

CREATE TABLE client_verification_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    token VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMPTZ
);

CREATE INDEX idx_client_verification_tokens_expires_at
    ON client_verification_tokens (expires_at);

CREATE TABLE operation_history (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    action VARCHAR(32) NOT NULL,
    details TEXT NOT NULL,
    organization_name VARCHAR(255) NOT NULL,
    success BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_operation_history_email_created_at
    ON operation_history (email, created_at DESC);

CREATE INDEX idx_operation_history_org_created_at
    ON operation_history (organization_name, created_at DESC);
