package org.verse.orgbridge.handler.contract;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public interface Organization {

    Mono<ServerResponse> create(ServerRequest request);

    Mono<ServerResponse> read(ServerRequest request);

    Mono<ServerResponse> delete(ServerRequest request);

    Mono<ServerResponse> getAll(ServerRequest request);
}
