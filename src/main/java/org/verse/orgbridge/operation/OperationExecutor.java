package org.verse.orgbridge.operation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.verse.orgbridge.cache.OrganizationCacheService;
import org.verse.orgbridge.csv.CsvFieldValidationService;
import org.verse.orgbridge.csv.CsvMetadataZipBuilder;
import org.verse.orgbridge.operation.contract.ComponentSelection;
import org.verse.orgbridge.operation.contract.OperationRequest;
import org.verse.orgbridge.records.auth.Credentials;
import org.verse.orgbridge.records.retrieve.RetrieveType;
import org.verse.orgbridge.service.MetadataService;
import org.verse.orgbridge.xml.DeployResponseParser;
import org.verse.orgbridge.xml.RetrieveZipExtractor;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperationExecutor {

    private final OperationRepository repository;
    private final OrganizationCacheService organizationCache;
    private final MetadataService metadataService;
    private final CsvFieldValidationService csvValidationService;
    private final CsvMetadataZipBuilder csvMetadataZipBuilder;
    private final ObjectMapper objectMapper;

    public Mono<Void> process(OperationJob job) {
        return Mono.fromCallable(() ->
                        objectMapper.readValue(
                                job.getRequestJson(),
                                OperationRequest.class
                        )
                )
                .flatMap(request -> execute(job, request))
                .flatMap(result -> complete(job, result))
                .onErrorResume(error -> fail(job, error));
    }

    private Mono<JsonNode> execute(
            OperationJob job,
            OperationRequest request
    ) {
        return switch (request.type()) {
            case COMPONENT_RETRIEVE -> retrieve(job, request);
            case COMPONENT_VALIDATE -> transfer(job, request, true);
            case COMPONENT_DEPLOY -> transfer(job, request, false);
            case CSV_VALIDATE -> deployCsv(job, request, true);
            case CSV_DEPLOY -> deployCsv(job, request, false);
        };
    }

    private Mono<JsonNode> retrieve(
            OperationJob job,
            OperationRequest request
    ) {
        return credentials(
                job.getTenantKey(),
                request.sourceOrganizationId()
        ).flatMap(credentials ->
                metadataService.retrieveMetadata(
                                job.getTenantKey(),
                                credentials,
                                retrieveTypes(request.components())
                        )
                        .thenReturn(objectMapper.createObjectNode()
                                .put("message", "Metadata retrieved")
                                .put("components", request.itemCount()))
        );
    }

    private Mono<JsonNode> transfer(
            OperationJob job,
            OperationRequest request,
            boolean checkOnly
    ) {
        Mono<Credentials> source = credentials(
                job.getTenantKey(),
                request.sourceOrganizationId()
        );
        Mono<Credentials> target = credentials(
                job.getTenantKey(),
                request.targetOrganizationId()
        );

        return Mono.zip(source, target)
                .flatMap(credentials ->
                        metadataService.retrieveMetadata(
                                        job.getTenantKey(),
                                        credentials.getT1(),
                                        retrieveTypes(request.components())
                                )
                                .flatMap(RetrieveZipExtractor::extractZipFile)
                                .flatMap(zip -> metadataService.deployMetadata(
                                        job.getTenantKey(),
                                        credentials.getT2(),
                                        zip,
                                        checkOnly
                                ))
                )
                .flatMap(DeployResponseParser::parse)
                .map(objectMapper::valueToTree);
    }

    private Mono<JsonNode> deployCsv(
            OperationJob job,
            OperationRequest request,
            boolean checkOnly
    ) {
        var validation = csvValidationService.validate(request.fields());

        if (!validation.valid()) {
            return Mono.error(new IllegalArgumentException(
                    "CSV validation failed with "
                            + validation.errorCount()
                            + " error(s)"
            ));
        }

        return Mono.zip(
                        credentials(
                                job.getTenantKey(),
                                request.targetOrganizationId()
                        ),
                        csvMetadataZipBuilder.build(request.fields())
                )
                .flatMap(tuple -> metadataService.deployMetadata(
                        job.getTenantKey(),
                        tuple.getT1(),
                        tuple.getT2(),
                        checkOnly
                ))
                .flatMap(DeployResponseParser::parse)
                .map(objectMapper::valueToTree);
    }

    private Mono<Credentials> credentials(
            String tenantKey,
            String organizationIdentifier
    ) {
        return organizationCache.read(tenantKey, organizationIdentifier)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Organization session is missing or expired: "
                                + organizationIdentifier
                )))
                .map(session -> Credentials.builder()
                        .org(
                                session.getOrganizationName() == null
                                        ? session.getOrganizationId()
                                        : session.getOrganizationName()
                        )
                        .sessionId(session.getSessionId())
                        .endpoint(session.getMetadataServerUrl() == null
                                ? session.getServerUrl()
                                : session.getMetadataServerUrl())
                        .build());
    }

    private List<RetrieveType> retrieveTypes(
            List<ComponentSelection> components
    ) {
        Map<String, List<String>> grouped = components.stream()
                .collect(Collectors.groupingBy(
                        ComponentSelection::type,
                        Collectors.mapping(
                                ComponentSelection::fullName,
                                Collectors.toList()
                        )
                ));

        return grouped.entrySet().stream()
                .map(entry -> RetrieveType.builder()
                        .name(entry.getKey())
                        .members(entry.getValue())
                        .build())
                .toList();
    }

    private Mono<Void> complete(
            OperationJob job,
            JsonNode result
    ) {
        job.setStatus(OperationStatus.SUCCEEDED.name());
        job.setProcessedItems(job.getTotalItems());
        job.setErrorCount(0);
        job.setCompletedAt(Instant.now());

        try {
            job.setResultJson(objectMapper.writeValueAsString(result));
        } catch (Exception error) {
            job.setResultJson("{\"message\":\"Operation completed\"}");
        }

        return repository.save(job).then();
    }

    private Mono<Void> fail(OperationJob job, Throwable error) {
        job.setStatus(OperationStatus.FAILED.name());
        job.setErrorCount(1);
        job.setErrorMessage(safeMessage(error));
        job.setCompletedAt(Instant.now());
        return repository.save(job).then();
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return "Operation failed";
        }
        return message.length() > 2000
                ? message.substring(0, 2000)
                : message;
    }
}
