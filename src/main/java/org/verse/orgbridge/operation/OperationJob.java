package org.verse.orgbridge.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("operation_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationJob {

    @Id
    private String id;

    private String tenantKey;
    private String salesforceOrgId;
    private String salesforceUserId;
    private String correlationId;
    private String operationType;
    private String status;
    private String sourceOrganizationId;
    private String targetOrganizationId;
    private String requestJson;
    private String resultJson;
    private String errorMessage;
    private int totalItems;
    private int processedItems;
    private int errorCount;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;

    @Version
    private Long version;

    public OperationType type() {
        return OperationType.valueOf(operationType);
    }

    public OperationStatus operationStatus() {
        return OperationStatus.valueOf(status);
    }
}
