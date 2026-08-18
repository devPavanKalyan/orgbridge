CREATE TABLE operation_jobs (
    id VARCHAR(36) PRIMARY KEY,
    tenant_key VARCHAR(80) NOT NULL,
    salesforce_org_id VARCHAR(18) NOT NULL,
    salesforce_user_id VARCHAR(18) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    operation_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    source_organization_id VARCHAR(64),
    target_organization_id VARCHAR(64),
    request_json TEXT NOT NULL,
    result_json TEXT,
    error_message TEXT,
    total_items INTEGER NOT NULL DEFAULT 0,
    processed_items INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    version BIGINT
);

CREATE INDEX idx_operation_jobs_tenant_created
    ON operation_jobs (tenant_key, created_at DESC);

CREATE INDEX idx_operation_jobs_queue
    ON operation_jobs (status, created_at)
    WHERE status = 'QUEUED';

CREATE INDEX idx_operation_jobs_correlation
    ON operation_jobs (correlation_id);
