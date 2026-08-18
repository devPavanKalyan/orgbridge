package org.verse.orgbridge.operation.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.verse.orgbridge.operation.OperationJob;

import java.time.Instant;

public record OperationResponse(
        String id,
        String correlationId,
        String type,
        String status,
        String sourceOrganizationId,
        String targetOrganizationId,
        int totalItems,
        int processedItems,
        int errorCount,
        String errorMessage,
        JsonNode result,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt
) {
    public static OperationResponse from(
            OperationJob job,
            JsonNode result
    ) {
        return new OperationResponse(
                job.getId(),
                job.getCorrelationId(),
                job.getOperationType(),
                job.getStatus(),
                job.getSourceOrganizationId(),
                job.getTargetOrganizationId(),
                job.getTotalItems(),
                job.getProcessedItems(),
                job.getErrorCount(),
                job.getErrorMessage(),
                result,
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }
}
