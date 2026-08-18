package org.verse.orgbridge.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.verse.orgbridge.cache.OrganizationCacheService;
import org.verse.orgbridge.records.auth.Credentials;
import org.verse.orgbridge.records.deploy.DeployPayload;
import org.verse.orgbridge.records.retrieve.RetrievePayload;
import org.verse.orgbridge.records.types.TypesRequestPayload;
import org.verse.orgbridge.service.MetadataService;
import org.verse.orgbridge.xml.DeployResponseParser;
import org.verse.orgbridge.xml.RetrieveZipExtractor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class MetadataHandler {

    private final MetadataService metadataService;
    private final OrganizationCacheService cacheService;

    public Mono<ServerResponse> fetchMetadataTypes(ServerRequest request) {
        return doAuthenticate(request)
                .flatMap(auth ->
                        request.bodyToMono(TypesRequestPayload.class)
                                .flatMap(payload ->
                                        doValidate(auth.getName(), payload.userId())
                                                .flatMap(metadataService::listMetadataTypes)
                                )
                )
                .flatMap(result ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(result)
                );
    }

    public Mono<ServerResponse> fetchMetadataComponents(ServerRequest request) {
        return doAuthenticate(request)
                .flatMap(auth ->
                        request.bodyToMono(TypesRequestPayload.class)
                                .flatMap(payload ->
                                        doValidate(auth.getName(), payload.userId())
                                                .flatMap(credentials ->
                                                        metadataService.listMetadataComponents(payload, credentials)
                                                )
                                )
                )
                .flatMap(result ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(result)
                );
    }

    public Mono<ServerResponse> pullMetadata(ServerRequest request) {
        return doAuthenticate(request)
                .flatMap(auth ->
                        request.bodyToMono(RetrievePayload.class)
                                .flatMap(payload ->
                                        doValidate(auth.getName(), payload.getUserId())
                                                .flatMap(credentials ->
                                                        metadataService.retrieveMetadata(
                                                                auth.getName(),
                                                                credentials,
                                                                payload.getTypes()
                                                        )
                                                )
                                )
                )
                .flatMap(xml ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_XML)
                                .bodyValue(xml)
                );
    }

    public Mono<ServerResponse> pushMetadata(ServerRequest request) {
        return doAuthenticate(request)
                .flatMap(auth ->
                        request.bodyToMono(DeployPayload.class)
                                .flatMap(payload ->
                                        doValidate(auth.getName(), payload.userId())
                                                .flatMap(credentials ->
                                                        metadataService.deployMetadata(
                                                                auth.getName(),
                                                                credentials,
                                                                payload.base64()
                                                        )
                                                )
                                )
                )
                .flatMap(xml ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_XML)
                                .bodyValue(xml)
                );
    }

    public Mono<ServerResponse> executeMetadataDeployment(ServerRequest request) {
        return doAuthenticate(request)
                .flatMap(auth ->
                        request.bodyToMono(RetrievePayload.class)
                                .flatMap(payload ->
                                        doValidate(auth.getName(), payload.getUserId())
                                                .flatMap(credentials ->
                                                        metadataService.retrieveMetadata(
                                                                        auth.getName(),
                                                                        credentials,
                                                                        payload.getTypes()
                                                                )
                                                                .flatMap(RetrieveZipExtractor::extractZipFile)
                                                                .flatMap(zip ->
                                                                        metadataService.
                                                                                deployMetadata(
                                                                                        auth.getName(),
                                                                                        credentials,
                                                                                        zip
                                                                                )
                                                                )
                                                )
                                )
                )
                .flatMap(DeployResponseParser::parse)
                .flatMap(result ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(result)
                )
                .onErrorResume(ex ->
                        ServerResponse.status(500)
                                .bodyValue("Retrieve & Deploy failed: " + ex.getMessage())
                );
    }

    public Mono<Authentication> doAuthenticate(ServerRequest request) {
        return request.principal()
                .switchIfEmpty(
                        Mono.error(
                                new InsufficientAuthenticationException("Authentication is required")
                        )
                ).cast(Authentication.class);
    }

    public Mono<Credentials> doValidate(String userKey, String identifier) {
        return cacheService.read(userKey, identifier)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid or expired session")))
                .map(session ->
                        Credentials.builder()
                                .org(session.getUserEmail())
                                .sessionId(session.getSessionId())
                                .endpoint(session.getServerUrl())
                                .build()
                );
    }


}
