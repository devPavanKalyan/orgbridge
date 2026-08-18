package org.verse.orgbridge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.verse.orgbridge.model.Action;
import org.verse.orgbridge.model.History;
import org.verse.orgbridge.records.auth.Credentials;
import org.verse.orgbridge.records.retrieve.RetrieveType;
import org.verse.orgbridge.records.types.TypesRequestPayload;
import org.verse.orgbridge.utils.AsyncResultParser;
import org.verse.orgbridge.utils.Helpers;
import org.verse.orgbridge.xml.MetadataRequestBuilder;
import org.verse.orgbridge.xml.MetadataResponseParser;
import org.verse.orgbridge.xml.DeployResponseParser;
import org.verse.orgbridge.xml.RetrieveRequestFactory;
import org.verse.orgbridge.exceptions.SalesforceAuthException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.verse.orgbridge.xml.MetadataRequestBuilder.describe;
import static org.verse.orgbridge.xml.MetadataRequestBuilder.list;

@Component
@RequiredArgsConstructor
public class MetadataService {

    private static final Duration RETRIEVE_POLL_INTERVAL = Duration.ofSeconds(1);
    private static final Duration DEPLOY_POLL_INTERVAL = Duration.ofSeconds(2);
    private static final Duration POLL_TIMEOUT = Duration.ofMinutes(5);
    private final HistoryService historyService;
    private final WebClient webClient;

    public Mono<String> retrieveMetadata(
            String userEmail,
            Credentials credentials,
            List<RetrieveType> retrieveTypes
    ) {
        return executeSoapRequest(
                Helpers.transformToDataEndpoint(credentials.endpoint()),
                RetrieveRequestFactory.create(
                        credentials,
                        retrieveTypes
                )
        )
                .flatMap(AsyncResultParser::extractAsyncId)
                .flatMap(retrieveId ->
                        pollRetrieveCompletion(
                                Helpers.transformToDataEndpoint(credentials.endpoint()),
                                credentials.sessionId(),
                                retrieveId
                        )
                )
                .flatMap(result ->
                        historyService.create(
                                        credentials.org(),
                                        History.builder()
                                                .email(userEmail)
                                                .action(Action.RETRIEVE)
                                                .success(true)
                                                .org(credentials.org())
                                                .details("SUCCESS")
                                                .build()
                                )
                                .thenReturn(result)
                )
                .onErrorResume(ex ->
                        historyService.create(
                                        credentials.org(),
                                        History.builder()
                                                .email(userEmail)
                                                .action(Action.RETRIEVE)
                                                .success(false)
                                                .org(credentials.org())
                                                .details(ex.getMessage())
                                                .build()
                                )
                                .then(Mono.error(ex))
                );
    }

    public Mono<List<String>> listMetadataTypes(Credentials credentials) {
        String url = Helpers.transformToDataEndpoint(credentials.endpoint());
        return executeSoapRequest(
                url,
                describe(
                        credentials.sessionId()
                )
        ).map(MetadataResponseParser::parseTypes);
    }

    public Mono<Object> listMetadataComponents(
            TypesRequestPayload payload,
            Credentials credentials
    ) {

        return executeSoapRequest(
                Helpers.transformToDataEndpoint(credentials.endpoint()),
                list(
                        credentials.sessionId(),
                        payload.type()
                )
        ).map(MetadataResponseParser::parseComponents);
    }

    public Mono<String> deployMetadata(
            String userEmail,
            Credentials credentials,
            String zipBase64
    ) {
        return deployMetadata(userEmail, credentials, zipBase64, false);
    }

    public Mono<String> deployMetadata(
            String userEmail,
            Credentials credentials,
            String zipBase64,
            boolean checkOnly
    ) {
        return executeSoapRequest(
                Helpers.transformToDataEndpoint(credentials.endpoint()),
                MetadataRequestBuilder.deploy(
                        credentials.sessionId(),
                        zipBase64,
                        checkOnly
                )
        )
                .flatMap(AsyncResultParser::extractAsyncId)
                .flatMap(deployId ->
                        pollDeployCompletion(
                                Helpers.transformToDataEndpoint(credentials.endpoint()),
                                credentials.sessionId(),
                                deployId
                        )
                )
                .flatMap(result ->
                        DeployResponseParser.parse(result)
                                .flatMap(parsed -> {
                                    if (!parsed.isSuccess()) {
                                        return Mono.error(
                                                new IllegalStateException(
                                                        "Salesforce operation failed with status "
                                                                + parsed.getStatus()
                                                )
                                        );
                                    }
                                    return historyService.create(
                                                    credentials.org(),
                                                    History.builder()
                                                            .email(userEmail)
                                                            .action(
                                                                    checkOnly
                                                                            ? Action.VALIDATE
                                                                            : Action.DEPLOY
                                                            )
                                                            .success(true)
                                                            .org(credentials.org())
                                                            .details("SUCCESS")
                                                            .build()
                                            )
                                            .thenReturn(result);
                                })
                )
                .onErrorResume(ex ->
                        historyService.create(
                                        credentials.org(),
                                        History.builder()
                                                .email(userEmail)
                                                .action(
                                                        checkOnly
                                                                ? Action.VALIDATE
                                                                : Action.DEPLOY
                                                )
                                                .success(false)
                                                .org(credentials.org())
                                                .details(ex.getMessage())
                                                .build()
                                )
                                .then(Mono.error(ex))
                );
    }

    private Mono<String> pollRetrieveCompletion(
            String endpoint,
            String sessionId,
            String retrieveId
    ) {
        return Flux.interval(RETRIEVE_POLL_INTERVAL)
                .concatMap(tick ->
                        executeSoapRequest(
                                endpoint,
                                MetadataRequestBuilder.checkRetrieve(
                                        sessionId,
                                        retrieveId
                                )
                        )
                )
                .filter(AsyncResultParser::isCompleted)
                .next()
                .timeout(POLL_TIMEOUT);
    }

    private Mono<String> pollDeployCompletion(
            String endpoint,
            String sessionId,
            String deployId
    ) {
        return Flux.interval(DEPLOY_POLL_INTERVAL)
                .concatMap(tick ->
                        executeSoapRequest(
                                endpoint,
                                MetadataRequestBuilder.checkDeploy(
                                        sessionId,
                                        deployId
                                )
                        )
                )
                .filter(AsyncResultParser::isCompleted)
                .next()
                .timeout(POLL_TIMEOUT);
    }


    private Mono<String> executeSoapRequest(
            String endpoint,
            String requestXml
    ) {
        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .header("SOAPAction", "\"\"")
                .bodyValue(requestXml)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("Salesforce request failed")
                                .flatMap(body -> Mono.error(
                                        new SalesforceAuthException(
                                                "SALESFORCE_HTTP_"
                                                        + response.statusCode().value(),
                                                body
                                        )
                                ))
                )
                .bodyToMono(String.class)
                .doOnNext(
                        org.verse.orgbridge.exceptions.parser
                                .SalesforceSoapFaultParser::throwIfFault
                )
                .timeout(Duration.ofSeconds(60));
    }
}
