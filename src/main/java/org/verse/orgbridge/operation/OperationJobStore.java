package org.verse.orgbridge.operation;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OperationJobStore {

    private final DatabaseClient databaseClient;

    public Mono<OperationJob> claimNext() {
        return databaseClient.sql("""
                        WITH next_job AS (
                            SELECT id
                            FROM operation_jobs
                            WHERE status = 'QUEUED'
                            ORDER BY created_at
                            FOR UPDATE SKIP LOCKED
                            LIMIT 1
                        )
                        UPDATE operation_jobs jobs
                        SET status = 'RUNNING',
                            started_at = :startedAt,
                            version = COALESCE(version, 0) + 1
                        FROM next_job
                        WHERE jobs.id = next_job.id
                        RETURNING jobs.*
                        """)
                .bind("startedAt", Instant.now())
                .map((row, metadata) -> OperationJob.builder()
                        .id(row.get("id", String.class))
                        .tenantKey(row.get("tenant_key", String.class))
                        .salesforceOrgId(row.get("salesforce_org_id", String.class))
                        .salesforceUserId(row.get("salesforce_user_id", String.class))
                        .correlationId(row.get("correlation_id", String.class))
                        .operationType(row.get("operation_type", String.class))
                        .status(row.get("status", String.class))
                        .sourceOrganizationId(
                                row.get("source_organization_id", String.class)
                        )
                        .targetOrganizationId(
                                row.get("target_organization_id", String.class)
                        )
                        .requestJson(row.get("request_json", String.class))
                        .resultJson(row.get("result_json", String.class))
                        .errorMessage(row.get("error_message", String.class))
                        .totalItems(value(row.get("total_items", Integer.class)))
                        .processedItems(value(row.get("processed_items", Integer.class)))
                        .errorCount(value(row.get("error_count", Integer.class)))
                        .createdAt(row.get("created_at", Instant.class))
                        .startedAt(row.get("started_at", Instant.class))
                        .completedAt(row.get("completed_at", Instant.class))
                        .version(row.get("version", Long.class))
                        .build())
                .one();
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }
}
