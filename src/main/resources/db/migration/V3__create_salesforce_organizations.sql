CREATE TABLE salesforce_organizations (
    id VARCHAR(36) PRIMARY KEY,
    tenant_key VARCHAR(80) NOT NULL,
    salesforce_organization_id VARCHAR(18),
    name VARCHAR(160) NOT NULL,
    username VARCHAR(320) NOT NULL,
    environment VARCHAR(16) NOT NULL,
    login_url VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'Needs Verification',
    active BOOLEAN NOT NULL DEFAULT FALSE,
    last_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_salesforce_organizations_tenant_name
    ON salesforce_organizations (tenant_key, LOWER(name));

CREATE UNIQUE INDEX uq_salesforce_organizations_tenant_username
    ON salesforce_organizations (tenant_key, LOWER(username));

CREATE UNIQUE INDEX uq_salesforce_organizations_active
    ON salesforce_organizations (tenant_key)
    WHERE active = TRUE;

CREATE INDEX idx_salesforce_organizations_tenant_created
    ON salesforce_organizations (tenant_key, created_at);

CREATE INDEX idx_salesforce_organizations_salesforce_id
    ON salesforce_organizations (tenant_key, salesforce_organization_id);
