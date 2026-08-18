package org.verse.orgbridge.handler.base;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.security.Principal;

import static org.verse.orgbridge.utils.Helpers.doAuthenticate;

public abstract class AbstractSessionHandler {

    protected Mono<String> authenticatedUser(ServerRequest request) {
        return doAuthenticate(request).map(Principal::getName);
    }

    protected String identifier(ServerRequest request) {
        return request.pathVariable("identifier");
    }

    protected Mono<ServerResponse> ok(Object body) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    protected Mono<ServerResponse> notFound() {
        return ServerResponse.notFound().build();
    }
}
