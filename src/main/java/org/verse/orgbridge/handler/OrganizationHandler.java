package org.verse.orgbridge.handler;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.verse.orgbridge.cache.OrganizationCacheService;
import org.verse.orgbridge.exceptions.AuthenticationFailedException;
import org.verse.orgbridge.exceptions.SalesforceAuthException;
import org.verse.orgbridge.exceptions.parser.SalesforceSoapFaultParser;
import org.verse.orgbridge.handler.base.AbstractSessionHandler;
import org.verse.orgbridge.handler.contract.Organization;
import org.verse.orgbridge.records.auth.OAuthCredRequest;
import org.verse.orgbridge.records.session.SalesforceSession;
import org.verse.orgbridge.utils.Helpers;
import org.verse.orgbridge.vault.VaultService;
import org.verse.orgbridge.xml.LoginRequestBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class OrganizationHandler
        extends AbstractSessionHandler
        implements Organization {

    private final OrganizationCacheService cache;
    private final VaultService vault;
    private final WebClient client;

    public OrganizationHandler(OrganizationCacheService cache,
                               VaultService vault,
                               @Qualifier("organizationWebClient")
                               WebClient client) {
        this.cache = cache;
        this.vault = vault;
        this.client = client;
    }

    @Override
    public Mono<ServerResponse> create(ServerRequest request) {
        return authenticatedUser(request)
                .flatMap(authUserEmail ->
                        request.bodyToMono(OAuthCredRequest.class)
                                .flatMap(req -> authenticateAndStore(authUserEmail, req))
                )
                .flatMap(this::ok);
    }

    @Override
    public Mono<ServerResponse> getAll(ServerRequest request) {
        return authenticatedUser(request)
                .flatMap(user ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(cache.getAll(user), SalesforceSession.class)
                );
    }

    @Override
    public Mono<ServerResponse> read(ServerRequest request) {
        return authenticatedUser(request)
                .flatMap(user -> cache.read(user, identifier(request)))
                .flatMap(this::ok)
                .switchIfEmpty(notFound());
    }

    @Override
    public Mono<ServerResponse> delete(ServerRequest request) {
        return authenticatedUser(request)
                .flatMap(user -> cache.delete(user, identifier(request)))
                .flatMap(this::ok);
    }

    private Mono<Map<String, String>> authenticateAndStore(
            String authUserEmail,
            OAuthCredRequest req
    ) {

        String soapBody = LoginRequestBuilder.build(
                req.getUsername(),
                req.getPassword(),
                req.getSecurityToken()
        );

        String url = Helpers.transformToActualEndpoint(authUrl(req.getOrgType()));


        return client.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, "text/xml; charset=UTF-8")
                .header(HttpHeaders.ACCEPT, "text/xml")
                .header("SOAPAction", "login")
                .bodyValue(soapBody)

                .retrieve()

                .onStatus(
                        HttpStatusCode::isError,
                        response -> response
                                .bodyToMono(String.class)
                                .defaultIfEmpty("Salesforce service unavailable")
                                .flatMap(body -> {
                                    SalesforceSoapFaultParser.throwIfFault(body);
                                    return Mono.error(
                                            new SalesforceAuthException(
                                                    "HTTP_" + response.statusCode().value(),
                                                    "Salesforce service error"
                                            )
                                    );
                                })
                )

                .bodyToMono(String.class)
                .doOnNext(SalesforceSoapFaultParser::throwIfFault)

                .map(LoginRequestBuilder::parse)

                .switchIfEmpty(Mono.error(
                        new AuthenticationFailedException("Invalid credentials")
                ))

                .flatMap(session -> {
                    if (!session.isAuthenticated()) {
                        return Mono.error(
                                new AuthenticationFailedException(
                                        "Invalid username, password, or security token"
                                )
                        );
                    }

                    return cache.create(authUserEmail, session)
                            .thenReturn(response(session));
                });
    }


    private Map<String, String> response(SalesforceSession s) {
        return Map.of(
                "status", "SUCCESS",
                "organizationId", s.getOrganizationId(),
                "userEmail", s.getUserEmail()
        );
    }

    private String authUrl(String type) {
        return "sandbox".equalsIgnoreCase(type)
                ? vault.sandbox()
                : vault.production();
    }
}
