package org.verse.orgbridge.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.verse.orgbridge.cache.OrganizationCacheService;
import org.verse.orgbridge.csv.CsvFieldValidationService;
import org.verse.orgbridge.integration.contract.CsvValidationRequest;
import org.verse.orgbridge.integration.contract.MetadataComponentsRequest;
import org.verse.orgbridge.integration.contract.OrganizationSummary;
import org.verse.orgbridge.operation.OperationService;
import org.verse.orgbridge.operation.contract.OperationRequest;
import org.verse.orgbridge.organization.OrganizationProfileRequest;
import org.verse.orgbridge.organization.OrganizationProfileService;
import org.verse.orgbridge.records.auth.Credentials;
import org.verse.orgbridge.records.types.TypesRequestPayload;
import org.verse.orgbridge.service.MetadataService;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IntegrationHandler {

    private final OperationService operationService;
    private final OrganizationCacheService organizationCache;
    private final OrganizationProfileService organizationProfileService;
    private final MetadataService metadataService;
    private final CsvFieldValidationService csvValidationService;

    public Mono<ServerResponse> dashboard(ServerRequest request) {
        return context(request)
                .flatMap(context -> Mono.zip(
                                operationService.dashboard(context),
                                organizationProfileService.count(context)
                        )
                        .flatMap(values -> {
                            Map<String, Object> response =
                                    new LinkedHashMap<>(values.getT1());
                            response.put("organizations", values.getT2());
                            return json(response);
                        }));
    }

    public Mono<ServerResponse> organizations(ServerRequest request) {
        return context(request)
                .flatMap(context -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                organizationProfileService.getAll(context),
                                OrganizationSummary.class
                        ));
    }

    public Mono<ServerResponse> createOrganization(
            ServerRequest request
    ) {
        return Mono.zip(
                        context(request),
                        request.bodyToMono(OrganizationProfileRequest.class)
                )
                .flatMap(tuple -> organizationProfileService.create(
                        tuple.getT1(),
                        tuple.getT2()
                ))
                .flatMap(response -> ServerResponse
                        .status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> updateOrganization(
            ServerRequest request
    ) {
        return Mono.zip(
                        context(request),
                        request.bodyToMono(OrganizationProfileRequest.class)
                )
                .flatMap(tuple -> organizationProfileService.update(
                        tuple.getT1(),
                        request.pathVariable("id"),
                        tuple.getT2()
                ))
                .flatMap(this::json);
    }

    public Mono<ServerResponse> deleteOrganization(
            ServerRequest request
    ) {
        return context(request)
                .flatMap(context -> organizationProfileService.delete(
                        context,
                        request.pathVariable("id")
                ))
                .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> activateOrganization(
            ServerRequest request
    ) {
        return context(request)
                .flatMap(context -> organizationProfileService.activate(
                        context,
                        request.pathVariable("id")
                ))
                .flatMap(this::json);
    }

    public Mono<ServerResponse> testOrganization(
            ServerRequest request
    ) {
        return context(request)
                .flatMap(context ->
                        organizationProfileService.testConnection(
                                context,
                                request.pathVariable("id")
                        )
                )
                .flatMap(this::json);
    }

    public Mono<ServerResponse> metadataTypes(ServerRequest request) {
        String organizationId = requiredQuery(
                request,
                "organizationId"
        );

        return context(request)
                .flatMap(context -> credentials(context, organizationId))
                .flatMap(metadataService::listMetadataTypes)
                .flatMap(this::json);
    }

    public Mono<ServerResponse> metadataComponents(
            ServerRequest request
    ) {
        return Mono.zip(
                        context(request),
                        request.bodyToMono(MetadataComponentsRequest.class)
                )
                .flatMap(tuple -> credentials(
                                tuple.getT1(),
                                tuple.getT2().organizationId()
                        )
                        .flatMap(credentials ->
                                metadataService.listMetadataComponents(
                                        TypesRequestPayload.builder()
                                                .userId(
                                                        tuple.getT2()
                                                                .organizationId()
                                                )
                                                .type(tuple.getT2().type())
                                                .build(),
                                        credentials
                                )
                        ))
                .flatMap(this::json);
    }

    public Mono<ServerResponse> validateCsv(ServerRequest request) {
        return request.bodyToMono(CsvValidationRequest.class)
                .map(CsvValidationRequest::fields)
                .map(csvValidationService::validate)
                .flatMap(this::json);
    }

    public Mono<ServerResponse> submitOperation(ServerRequest request) {
        return Mono.zip(
                        context(request),
                        request.bodyToMono(OperationRequest.class)
                )
                .flatMap(tuple ->
                        operationService.submit(tuple.getT1(), tuple.getT2())
                )
                .flatMap(response -> ServerResponse
                        .status(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> operations(ServerRequest request) {
        return context(request)
                .flatMap(context -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                operationService.getAll(context),
                                org.verse.orgbridge.operation.contract
                                        .OperationResponse.class
                        ));
    }

    public Mono<ServerResponse> operation(ServerRequest request) {
        return context(request)
                .flatMap(context -> operationService.get(
                        context,
                        request.pathVariable("id")
                ))
                .flatMap(this::json);
    }

    private Mono<SalesforceRequestContext> context(
            ServerRequest request
    ) {
        return request.principal()
                .cast(Authentication.class)
                .flatMap(SalesforceRequestContext::from);
    }

    private Mono<Credentials> credentials(
            SalesforceRequestContext context,
            String organizationId
    ) {
        return organizationCache.read(context.tenantKey(), organizationId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Organization session is missing or expired"
                )))
                .map(session -> Credentials.builder()
                        .org(session.getOrganizationName())
                        .sessionId(session.getSessionId())
                        .endpoint(
                                session.getMetadataServerUrl() == null
                                        ? session.getServerUrl()
                                        : session.getMetadataServerUrl()
                        )
                        .build());
    }

    private String requiredQuery(
            ServerRequest request,
            String name
    ) {
        return request.queryParam(name)
                .filter(value -> !value.isBlank())
                .orElseThrow(() ->
                        new IllegalArgumentException(name + " is required")
                );
    }

    private Mono<ServerResponse> json(Object value) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(value);
    }
}
