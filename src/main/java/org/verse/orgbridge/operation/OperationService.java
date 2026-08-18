package org.verse.orgbridge.operation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.verse.orgbridge.integration.SalesforceRequestContext;
import org.verse.orgbridge.operation.contract.OperationRequest;
import org.verse.orgbridge.operation.contract.OperationResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationRepository repository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public Mono<OperationResponse> submit(
            SalesforceRequestContext context,
            OperationRequest request
    ) {
        validate(request);

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(request))
                .map(requestJson -> OperationJob.builder()
                        .id(UUID.randomUUID().toString())
                        .tenantKey(context.tenantKey())
                        .salesforceOrgId(context.organizationId())
                        .salesforceUserId(context.userId())
                        .correlationId(context.correlationId())
                        .operationType(request.type().name())
                        .status(OperationStatus.QUEUED.name())
                        .sourceOrganizationId(request.sourceOrganizationId())
                        .targetOrganizationId(request.targetOrganizationId())
                        .requestJson(requestJson)
                        .totalItems(request.itemCount())
                        .processedItems(0)
                        .errorCount(0)
                        .createdAt(Instant.now())
                        .build())
                .flatMap(repository::save)
                .map(job -> OperationResponse.from(job, null));
    }

    public Mono<OperationResponse> get(
            SalesforceRequestContext context,
            String id
    ) {
        return repository.findByIdAndTenantKey(id, context.tenantKey())
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Operation was not found")
                ))
                .map(this::toResponse);
    }

    public Flux<OperationResponse> getAll(
            SalesforceRequestContext context
    ) {
        return repository
                .findByTenantKeyOrderByCreatedAtDesc(context.tenantKey())
                .take(200)
                .map(this::toResponse);
    }

    public Mono<Map<String, Long>> dashboard(
            SalesforceRequestContext context
    ) {
        return Mono.zip(
                repository.countByTenantKey(context.tenantKey()),
                repository.countByTenantKeyAndStatus(
                        context.tenantKey(),
                        OperationStatus.SUCCEEDED.name()
                ),
                repository.countByTenantKeyAndStatus(
                        context.tenantKey(),
                        OperationStatus.FAILED.name()
                ),
                repository.countByTenantKeyAndStatus(
                        context.tenantKey(),
                        OperationStatus.RUNNING.name()
                )
        ).map(counts -> Map.of(
                "total", counts.getT1(),
                "succeeded", counts.getT2(),
                "failed", counts.getT3(),
                "running", counts.getT4()
        ));
    }

    private OperationResponse toResponse(OperationJob job) {
        JsonNode result = null;

        if (job.getResultJson() != null
                && !job.getResultJson().isBlank()) {
            try {
                result = objectMapper.readTree(job.getResultJson());
            } catch (JsonProcessingException ignored) {
                result = objectMapper.createObjectNode()
                        .put("message", "Stored result is unavailable");
            }
        }
        return OperationResponse.from(job, result);
    }

    private void validate(OperationRequest request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        if (request.itemCount() == 0) {
            throw new IllegalArgumentException(
                    "At least one component or CSV field is required"
            );
        }

        switch (request.type()) {
            case COMPONENT_RETRIEVE -> require(
                    request.sourceOrganizationId(),
                    "sourceOrganizationId"
            );
            case COMPONENT_VALIDATE, COMPONENT_DEPLOY -> {
                require(
                        request.sourceOrganizationId(),
                        "sourceOrganizationId"
                );
                require(
                        request.targetOrganizationId(),
                        "targetOrganizationId"
                );
            }
            case CSV_VALIDATE, CSV_DEPLOY -> require(
                    request.targetOrganizationId(),
                    "targetOrganizationId"
            );
        }
    }

    private void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
